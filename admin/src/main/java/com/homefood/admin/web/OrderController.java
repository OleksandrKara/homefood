package com.homefood.admin.web;

import com.homefood.admin.entity.Client;
import com.homefood.admin.entity.DeliveryType;
import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderStatus;
import com.homefood.admin.entity.Product;
import com.homefood.admin.pricing.OrderPricing;
import com.homefood.admin.repository.ClientRepository;
import com.homefood.admin.repository.DistrictRepository;
import com.homefood.admin.repository.OrderRepository;
import com.homefood.admin.repository.PaymentMethodRepository;
import com.homefood.admin.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    // The business operates in PST/PDT; the container runs in UTC, so "today"/"tomorrow"
    // must be computed in the business's zone, not the server's default zone.
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Los_Angeles");

    private static final List<OrderStatus> PROCESSED_STATUSES = List.of(OrderStatus.DONE, OrderStatus.CANCELLED);

    private static final String[] MONTHS_RU = {
            "", "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
    };
    private static final String[] DOW_RU = {
            "", "понедельник", "вторник", "среда", "четверг", "пятница", "суббота", "воскресенье"
    };

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final DistrictRepository districtRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    public String list(Model model) {
        List<Order> all = orderRepository.findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

        Map<String, Map<String, List<Order>>> grouped = new LinkedHashMap<>();
        Map<String, Integer> jarsByDate = new LinkedHashMap<>();
        List<Order> requested = new ArrayList<>();

        for (Order o : all) {
            if (o.isArchived()) {
                continue;
            }
            if (o.getStatus() == OrderStatus.REQUESTED) {
                requested.add(o);
                continue;
            }
            if (PROCESSED_STATUSES.contains(o.getStatus())) {
                continue; // already sold/cancelled - excluded from the "still owed" count below too
            }
            String dateLabel = formatDateLabel(o.getDeliveryDate());
            String subLabel = o.getDeliveryType() == DeliveryType.PICKUP
                    ? "🏠 Самовывоз"
                    : (o.getDistrict() != null && !o.getDistrict().isBlank() ? o.getDistrict() : "Без района");
            grouped.computeIfAbsent(dateLabel, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subLabel, k -> new ArrayList<>())
                    .add(o);
            jarsByDate.merge(dateLabel, o.getQuantity(), Integer::sum);
        }

        model.addAttribute("requestedOrders", requested);
        model.addAttribute("groupedOrders", grouped);
        model.addAttribute("jarsByDate", jarsByDate);
        model.addAttribute("soldTotal", orderRepository.sumTotalPriceByStatus(OrderStatus.DONE));
        model.addAttribute("expectedTotal", orderRepository.sumTotalPriceByStatus(OrderStatus.NEW));
        model.addAttribute("tipsTotal", orderRepository.sumTipAmountByStatus(OrderStatus.DONE));
        model.addAttribute("processedCount", orderRepository.countByArchivedFalseAndStatusIn(PROCESSED_STATUSES));
        model.addAttribute("archivedCount", orderRepository.countByArchivedTrue());
        return "orders/list";
    }

    @GetMapping("/processed")
    public String processed(Model model) {
        model.addAttribute("orders",
                orderRepository.findByArchivedFalseAndStatusInOrderByDeliveryDateDescCreatedAtDesc(PROCESSED_STATUSES));
        return "orders/processed";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("orders", orderRepository.findByArchivedTrueOrderByDeliveryDateDescCreatedAtDesc());
        return "orders/archive";
    }

    @PostMapping("/{id}/archive")
    public String archiveOrder(@PathVariable Long id, @RequestParam(defaultValue = "/orders") String returnTo) {
        Order order = orderRef(id);
        order.setArchived(true);
        orderRepository.save(order);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @PostMapping("/{id}/unarchive")
    public String unarchiveOrder(@PathVariable Long id, @RequestParam(defaultValue = "/orders/archive") String returnTo) {
        Order order = orderRef(id);
        order.setArchived(false);
        orderRepository.save(order);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Order order = new Order();
        order.setQuantity(1);
        addFormAttributes(model, null);
        model.addAttribute("order", order);
        return "orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, @RequestParam Long productId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model, null);
            return "orders/form";
        }
        Product product = productRef(productId);
        order.setClient(clientRef(clientId));
        order.setProduct(product);
        order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), order.getQuantity(), product.isTieredDiscountEnabled()));
        orderRepository.save(order);
        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Order order = orderRef(id);
        addFormAttributes(model, order.getProduct());
        model.addAttribute("order", order);
        return "orders/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, @RequestParam Long productId, Model model) {
        Order existing = orderRef(id);
        if (result.hasErrors()) {
            addFormAttributes(model, existing.getProduct());
            return "orders/form";
        }
        Product product = productRef(productId);
        order.setId(id);
        order.setCreatedAt(existing.getCreatedAt());
        order.setArchived(existing.isArchived());
        order.setClient(clientRef(clientId));
        order.setProduct(product);
        order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), order.getQuantity(), product.isTieredDiscountEnabled()));
        orderRepository.save(order);
        return "redirect:/orders";
    }

    private Order orderRef(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private Client clientRef(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }

    private Product productRef(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    /** Active products, plus the currently-assigned one even if it's been deactivated since. */
    private List<Product> selectableProducts(Product currentProduct) {
        List<Product> products = new ArrayList<>(productRepository.findAllByActiveTrueOrderByNameAsc());
        if (currentProduct != null && !currentProduct.isActive()) {
            products.add(currentProduct);
        }
        return products;
    }

    /**
     * @param currentProduct the order's currently-assigned product, if editing - included even
     *                       when inactive so the dropdown still shows/keeps it selected.
     */
    private void addFormAttributes(Model model, Product currentProduct) {
        model.addAttribute("clients", clientRepository.findAllByOrderByNameAsc());
        model.addAttribute("products", selectableProducts(currentProduct));
        model.addAttribute("deliveryTypes", DeliveryType.values());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("districts", districtRepository.findAllByOrderByNameAsc());
        model.addAttribute("paymentMethods", paymentMethodRepository.findAllByOrderByNameAsc());

        Set<String> addresses = new LinkedHashSet<>(clientRepository.findDistinctAddresses());
        addresses.addAll(orderRepository.findDistinctDeliveryAddresses());
        model.addAttribute("addressSuggestions", addresses);
    }

    private String formatDateLabel(LocalDate date) {
        if (date == null) {
            return "Без даты";
        }
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        if (date.equals(today)) {
            return "Сегодня";
        }
        if (date.equals(today.plusDays(1))) {
            return "Завтра";
        }
        return date.getDayOfMonth() + " " + MONTHS_RU[date.getMonthValue()] + ", " + DOW_RU[date.getDayOfWeek().getValue()];
    }

    /** Whitelist redirect target so a crafted form field can't send an admin off-app. */
    private String sanitizeReturnTo(String returnTo) {
        if ("/orders/processed".equals(returnTo) || "/orders/archive".equals(returnTo)) {
            return returnTo;
        }
        return "/orders";
    }
}
