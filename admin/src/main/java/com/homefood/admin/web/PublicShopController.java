package com.homefood.admin.web;

import com.homefood.admin.entity.*;
import com.homefood.admin.phone.PhoneNumbers;
import com.homefood.admin.pricing.OrderPricing;
import com.homefood.admin.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Public, unauthenticated storefront (see SecurityConfig permitAll for /shop/**).
 * Nginx maps the bare domain root to this page - see nginx/food.akluxnails.com.conf.
 * Also see nginx rate limiting (homefood_shop_reserve zone) for the network-level side
 * of abuse protection - this controller only handles what nginx can't see (per-phone limits,
 * the honeypot field, field lengths).
 */
@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class PublicShopController {

    private static final int MAX_RESERVATION_QUANTITY = 10;
    private static final int MAX_ON_DEMAND_QUANTITY = 20;
    private static final int MAX_RESERVATIONS_PER_PHONE_PER_HOUR = 3;
    private static final int MAX_NAME_LENGTH = 255;
    private static final int MAX_PHONE_LENGTH = 50;
    private static final int MAX_NOTES_LENGTH = 2000;

    private final ProductRepository productRepository;
    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;
    private final ShopSettingsRepository shopSettingsRepository;

    @GetMapping
    public String shop(@RequestParam(required = false) String reserved,
                        @RequestParam(required = false) String error,
                        Model model) {
        List<Product> allProducts = productRepository.findAllByOrderByNameAsc();
        List<Product> availableProducts = allProducts.stream().filter(Product::isActive).toList();
        // Inactive products are "made to order" - not in stock, but still orderable: the business
        // confirms feasibility/timing by phone before committing (see OrderStatus.REQUESTED).
        List<Product> onDemandProducts = allProducts.stream().filter(p -> !p.isActive()).toList();

        Map<Long, String> iconByProduct = new HashMap<>();
        for (Product p : allProducts) {
            iconByProduct.put(p.getId(), ProductIcons.iconFor(p.getName()));
        }

        model.addAttribute("allProducts", allProducts);
        model.addAttribute("availableProducts", availableProducts);
        model.addAttribute("onDemandProducts", onDemandProducts);
        model.addAttribute("iconByProduct", iconByProduct);
        model.addAttribute("maxQuantity", MAX_RESERVATION_QUANTITY);
        model.addAttribute("maxQuantityOnDemand", MAX_ON_DEMAND_QUANTITY);
        model.addAttribute("pickupAddress", shopSettingsRepository.getSingleton().getPickupAddress());
        model.addAttribute("reserved", reserved != null);
        model.addAttribute("error", error);
        return "shop/index";
    }

    /**
     * Accepts a whole cart in one submission: parallel productId[]/quantity[] arrays (one pair per
     * product the customer added), read as raw servlet parameters rather than @RequestParam
     * List<String> - Spring's collection conversion silently comma-splits a single-element list,
     * corrupting a lone numeric value with no error shown (see RecipeController for the original
     * bug this pattern avoids). One Client and one shared createdAt are used for every line, so the
     * whole cart is tied together as a single checkout for the client and for rate-limiting.
     */
    @PostMapping("/reserve")
    public String reserve(@RequestParam String name,
                           @RequestParam String phone,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate preferredDate,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime preferredTime,
                           @RequestParam(required = false) String website,
                           HttpServletRequest request) {
        // Honeypot: a real visitor never sees or fills this field (hidden off-screen).
        // Pretend success so an automated submitter has no signal it was caught.
        if (website != null && !website.isBlank()) {
            return "redirect:/shop?reserved=1";
        }

        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH
                || phone == null || phone.isBlank() || phone.length() > MAX_PHONE_LENGTH
                || (notes != null && notes.length() > MAX_NOTES_LENGTH)) {
            return "redirect:/shop?error=1";
        }

        String[] productIds = request.getParameterValues("productId");
        String[] quantities = request.getParameterValues("quantity");
        int rowCount = productIds == null ? 0 : productIds.length;

        Instant now = Instant.now();
        List<Order> newOrders = new ArrayList<>();
        Set<Long> seenProducts = new HashSet<>();

        for (int i = 0; i < rowCount; i++) {
            String productIdStr = productIds[i];
            String qtyStr = (quantities != null && i < quantities.length) ? quantities[i] : null;
            if (productIdStr == null || productIdStr.isBlank() || qtyStr == null || qtyStr.isBlank()) {
                continue; // an empty cart slot - just ignore it
            }
            Long productId;
            int quantity;
            try {
                productId = Long.valueOf(productIdStr.trim());
                quantity = Integer.parseInt(qtyStr.trim());
            } catch (NumberFormatException e) {
                return "redirect:/shop?error=1";
            }
            if (quantity < 1) {
                continue; // 0 means "not added to the cart"
            }
            if (!seenProducts.add(productId)) {
                return "redirect:/shop?error=1";
            }
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                return "redirect:/shop?error=1";
            }
            // Active/in-stock items are capped tighter (physical jars on hand); inactive items are
            // made-to-order and confirmed by phone anyway, so a higher cap is fine.
            int maxForProduct = product.isActive() ? MAX_RESERVATION_QUANTITY : MAX_ON_DEMAND_QUANTITY;
            if (quantity > maxForProduct) {
                return "redirect:/shop?error=1";
            }

            Order order = new Order();
            order.setProduct(product);
            order.setQuantity(quantity);
            order.setDeliveryType(DeliveryType.PICKUP);
            order.setStatus(OrderStatus.REQUESTED);
            order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), quantity, product.isTieredDiscountEnabled()));
            order.setDeliveryDate(preferredDate);
            order.setDeliveryTime(preferredTime);
            order.setCreatedAt(now);
            if (notes != null && !notes.isBlank()) {
                order.setNotes(notes.trim());
            }
            newOrders.add(order);
        }

        if (newOrders.isEmpty()) {
            return "redirect:/shop?error=1";
        }

        // Canonical form (see Client.normalizePhone) so the rate-limit lookup and the dedup lookup
        // below both match however staff/customers already have this number stored, regardless of
        // how it was formatted at the time.
        String canonicalPhone = PhoneNumbers.canonicalize(phone);
        Instant hourAgo = now.minus(1, ChronoUnit.HOURS);
        if (orderRepository.countByClientPhoneSince(canonicalPhone, hourAgo) >= MAX_RESERVATIONS_PER_PHONE_PER_HOUR) {
            return "redirect:/shop?error=1";
        }

        // Reuse the existing contact for this phone instead of creating a duplicate client every
        // time the same person orders again.
        Client client = findExistingClientByPhone(canonicalPhone);
        String submittedName = name.trim();
        if (client == null) {
            client = new Client();
            client.setName(submittedName);
            client.setPhone(canonicalPhone);
        } else if (client.getName() == null || submittedName.length() > client.getName().length()) {
            // Only overwrite the stored name when the freshly-typed one is longer: more likely to
            // be the complete/correct name, whether the stored one was truncated or just wrong. A
            // *shorter* new entry is more likely a quick retype and shouldn't shrink a fuller name
            // already on file.
            client.setName(submittedName);
        }
        clientRepository.save(client);

        for (Order order : newOrders) {
            order.setClient(client);
        }
        orderRepository.saveAll(newOrders);

        return "redirect:/shop?reserved=1";
    }

    /**
     * Lets the shop page greet a returning customer as soon as they finish typing their phone,
     * without waiting for full form submission. Deliberately returns only a boolean - never the
     * stored name - since this is an unauthenticated endpoint and echoing back "yes, this phone
     * belongs to <name>" would leak customer names to anyone probing phone numbers.
     */
    @GetMapping("/client-lookup")
    @ResponseBody
    public Map<String, Boolean> clientLookup(@RequestParam(required = false) String phone) {
        return Map.of("found", findExistingClientByPhone(phone) != null);
    }

    private Client findExistingClientByPhone(String phone) {
        String normalized = PhoneNumbers.normalize(phone);
        if (normalized.isEmpty()) {
            return null;
        }
        return clientRepository.findAll().stream()
                .filter(c -> normalized.equals(PhoneNumbers.normalize(c.getPhone())))
                .findFirst()
                .orElse(null);
    }
}
