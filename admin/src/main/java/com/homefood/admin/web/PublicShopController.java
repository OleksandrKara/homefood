package com.homefood.admin.web;

import com.homefood.admin.entity.*;
import com.homefood.admin.pricing.OrderPricing;
import com.homefood.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ProductionBatchRepository productionBatchRepository;
    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public String shop(@RequestParam(required = false) String reserved,
                        @RequestParam(required = false) String error,
                        Model model) {
        List<Product> allProducts = productRepository.findAllByOrderByNameAsc();
        List<Product> availableProducts = allProducts.stream().filter(Product::isActive).toList();
        // Inactive products are "made to order" - not in stock, but still orderable: the business
        // confirms feasibility/timing by phone before committing (see OrderStatus.REQUESTED).
        List<Product> onDemandProducts = allProducts.stream().filter(p -> !p.isActive()).toList();

        Map<Long, ProductionBatch> nextBatchByProduct = new HashMap<>();
        for (ProductionBatch batch : productionBatchRepository.findByStatusOrderByBatchDateAsc(BatchStatus.PLANNED)) {
            nextBatchByProduct.putIfAbsent(batch.getProduct().getId(), batch);
        }

        model.addAttribute("allProducts", allProducts);
        model.addAttribute("availableProducts", availableProducts);
        model.addAttribute("onDemandProducts", onDemandProducts);
        model.addAttribute("nextBatchByProduct", nextBatchByProduct);
        model.addAttribute("maxQuantity", MAX_RESERVATION_QUANTITY);
        model.addAttribute("maxQuantityOnDemand", MAX_ON_DEMAND_QUANTITY);
        model.addAttribute("reserved", reserved != null);
        model.addAttribute("error", error);
        return "shop/index";
    }

    @PostMapping("/reserve")
    public String reserve(@RequestParam String name,
                           @RequestParam String phone,
                           @RequestParam Long productId,
                           @RequestParam Integer quantity,
                           @RequestParam(required = false) String notes,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate preferredDate,
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime preferredTime,
                           @RequestParam(required = false) String website) {
        // Honeypot: a real visitor never sees or fills this field (hidden off-screen).
        // Pretend success so an automated submitter has no signal it was caught.
        if (website != null && !website.isBlank()) {
            return "redirect:/shop?reserved=1";
        }

        if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH
                || phone == null || phone.isBlank() || phone.length() > MAX_PHONE_LENGTH
                || (notes != null && notes.length() > MAX_NOTES_LENGTH)
                || quantity == null || quantity < 1) {
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

        String trimmedPhone = phone.trim();
        Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
        if (orderRepository.countByClientPhoneSince(trimmedPhone, hourAgo) >= MAX_RESERVATIONS_PER_PHONE_PER_HOUR) {
            return "redirect:/shop?error=1";
        }

        Client client = new Client();
        client.setName(name.trim());
        client.setPhone(trimmedPhone);
        clientRepository.save(client);

        Order order = new Order();
        order.setClient(client);
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setDeliveryType(DeliveryType.PICKUP);
        order.setStatus(OrderStatus.REQUESTED);
        order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), quantity));
        order.setDeliveryDate(preferredDate);
        order.setDeliveryTime(preferredTime);
        if (notes != null && !notes.isBlank()) {
            order.setNotes(notes.trim());
        }
        orderRepository.save(order);

        return "redirect:/shop?reserved=1";
    }
}
