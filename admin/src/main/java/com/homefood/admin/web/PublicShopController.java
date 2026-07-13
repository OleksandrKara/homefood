package com.homefood.admin.web;

import com.homefood.admin.entity.*;
import com.homefood.admin.pricing.OrderPricing;
import com.homefood.admin.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Public, unauthenticated storefront (see SecurityConfig permitAll for /shop/**).
 * Nginx maps the bare domain root to this page - see nginx/food.akluxnails.com.conf.
 */
@Controller
@RequestMapping("/shop")
@RequiredArgsConstructor
public class PublicShopController {

    private static final int MAX_RESERVATION_QUANTITY = 10;

    private final ProductRepository productRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public String shop(@RequestParam(required = false) String reserved,
                        @RequestParam(required = false) String error,
                        Model model) {
        List<Product> products = productRepository.findAllByOrderByNameAsc().stream()
                .filter(Product::isActive)
                .toList();

        Map<Long, ProductionBatch> nextBatchByProduct = new HashMap<>();
        for (ProductionBatch batch : productionBatchRepository.findByStatusOrderByBatchDateAsc(BatchStatus.PLANNED)) {
            nextBatchByProduct.putIfAbsent(batch.getProduct().getId(), batch);
        }

        model.addAttribute("products", products);
        model.addAttribute("nextBatchByProduct", nextBatchByProduct);
        model.addAttribute("maxQuantity", MAX_RESERVATION_QUANTITY);
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
                           @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime preferredTime) {
        if (name == null || name.isBlank() || phone == null || phone.isBlank()
                || quantity == null || quantity < 1 || quantity > MAX_RESERVATION_QUANTITY) {
            return "redirect:/shop?error=1";
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null || !product.isActive()) {
            return "redirect:/shop?error=1";
        }

        Client client = new Client();
        client.setName(name.trim());
        client.setPhone(phone.trim());
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
