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
import com.homefood.admin.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneId;
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

    @GetMapping
    public String list(Model model) {
        List<Order> all = orderRepository.findAllByOrderByDeliveryDateAscDeliveryTimeAsc();

        Map<String, Map<String, List<Order>>> grouped = new LinkedHashMap<>();
        List<Order> done = new java.util.ArrayList<>();

        for (Order o : all) {
            if (o.getStatus() == OrderStatus.DONE) {
                done.add(o);
                continue;
            }
            String dateLabel = formatDateLabel(o.getDeliveryDate());
            String subLabel = o.getDeliveryType() == DeliveryType.PICKUP
                    ? "🏠 Самовывоз"
                    : (o.getDistrict() != null && !o.getDistrict().isBlank() ? o.getDistrict() : "Без района");
            grouped.computeIfAbsent(dateLabel, k -> new LinkedHashMap<>())
                    .computeIfAbsent(subLabel, k -> new java.util.ArrayList<>())
                    .add(o);
        }

        model.addAttribute("groupedOrders", grouped);
        model.addAttribute("doneOrders", done);
        return "orders/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        Order order = new Order();
        order.setQuantity(1);
        addFormAttributes(model);
        model.addAttribute("order", order);
        return "orders/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, @RequestParam Long productId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "orders/form";
        }
        Product product = productRef(productId);
        order.setClient(clientRef(clientId));
        order.setProduct(product);
        order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), order.getQuantity()));
        orderRepository.save(order);
        return "redirect:/orders";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        addFormAttributes(model);
        model.addAttribute("order", order);
        return "orders/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("order") Order order, BindingResult result,
                          @RequestParam Long clientId, @RequestParam Long productId, Model model) {
        if (result.hasErrors()) {
            addFormAttributes(model);
            return "orders/form";
        }
        Order existing = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
        Product product = productRef(productId);
        order.setId(id);
        order.setCreatedAt(existing.getCreatedAt());
        order.setClient(clientRef(clientId));
        order.setProduct(product);
        order.setTotalPrice(OrderPricing.calculateTotal(product.getBasePrice(), order.getQuantity()));
        orderRepository.save(order);
        return "redirect:/orders";
    }

    private Client clientRef(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }

    private Product productRef(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
    }

    private void addFormAttributes(Model model) {
        model.addAttribute("clients", clientRepository.findAllByOrderByNameAsc());
        model.addAttribute("products", productRepository.findAllByOrderByNameAsc());
        model.addAttribute("deliveryTypes", DeliveryType.values());
        model.addAttribute("statuses", OrderStatus.values());
        model.addAttribute("districts", districtRepository.findAllByOrderByNameAsc());

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
}
