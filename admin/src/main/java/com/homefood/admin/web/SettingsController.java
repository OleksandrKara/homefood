package com.homefood.admin.web;

import com.homefood.admin.entity.ShopSettings;
import com.homefood.admin.repository.ShopSettingsRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/** One settings page for the public shop's site-wide details (currently just the pickup address,
 * shown once on the public page rather than repeated per product - see ShopSettings). */
@Controller
@RequestMapping("/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final ShopSettingsRepository shopSettingsRepository;

    @GetMapping
    public String edit(Model model) {
        model.addAttribute("settings", shopSettingsRepository.getSingleton());
        return "settings/form";
    }

    @PostMapping
    public String update(@Valid @ModelAttribute("settings") ShopSettings settings, BindingResult result) {
        if (result.hasErrors()) {
            return "settings/form";
        }
        settings.setId(1L);
        shopSettingsRepository.save(settings);
        return "redirect:/settings?saved=1";
    }
}
