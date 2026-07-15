package com.homefood.admin.web;

import com.homefood.admin.entity.PaymentMethod;
import com.homefood.admin.repository.PaymentMethodRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodRepository paymentMethodRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("paymentMethods", paymentMethodRepository.findAllByOrderByNameAsc());
        return "payment-methods/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("paymentMethod", new PaymentMethod());
        return "payment-methods/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("paymentMethod") PaymentMethod paymentMethod, BindingResult result) {
        if (result.hasErrors()) {
            return "payment-methods/form";
        }
        paymentMethodRepository.save(paymentMethod);
        return "redirect:/payment-methods";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        PaymentMethod paymentMethod = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment method not found: " + id));
        model.addAttribute("paymentMethod", paymentMethod);
        return "payment-methods/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("paymentMethod") PaymentMethod paymentMethod, BindingResult result) {
        if (result.hasErrors()) {
            return "payment-methods/form";
        }
        paymentMethod.setId(id);
        paymentMethodRepository.save(paymentMethod);
        return "redirect:/payment-methods";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        paymentMethodRepository.deleteById(id);
        return "redirect:/payment-methods";
    }

    /**
     * Add a payment method from a modal on another page (e.g. the order form) without navigating
     * away. Reuses an existing method with the same name (case-insensitively) instead of erroring
     * on the unique constraint.
     */
    @PostMapping(value = "/quick", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickCreate(@RequestParam String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Укажите название"));
        }
        if (trimmed.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Слишком длинное название"));
        }
        PaymentMethod paymentMethod = paymentMethodRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    PaymentMethod pm = new PaymentMethod();
                    pm.setName(trimmed);
                    return paymentMethodRepository.save(pm);
                });
        return ResponseEntity.ok(Map.of("id", paymentMethod.getId(), "name", paymentMethod.getName()));
    }
}
