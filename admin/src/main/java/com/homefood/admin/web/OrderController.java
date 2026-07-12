package com.homefood.admin.web;

import com.homefood.admin.entity.Client;
import com.homefood.admin.entity.DeliveryType;
import com.homefood.admin.entity.Order;
import com.homefood.admin.entity.OrderStatus;
import com.homefood.admin.entity.Product;
import com.homefood.admin.repository.ClientRepository;
import com.homefood.admin.repository.OrderRepository;
import com.homefood.admin.repository.ProductRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderRepository orderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("orders", orderRepository.findAllByOrderByDeliveryDateAscDeliveryTimeAsc());
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
        order.setClient(clientRef(clientId));
        order.setProduct(productRef(productId));
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
        order.setId(id);
        order.setCreatedAt(existing.getCreatedAt());
        order.setClient(clientRef(clientId));
        order.setProduct(productRef(productId));
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
    }
}
