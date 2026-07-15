package com.homefood.admin.web;

import com.homefood.admin.entity.District;
import com.homefood.admin.repository.DistrictRepository;
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
@RequestMapping("/districts")
@RequiredArgsConstructor
public class DistrictController {

    private final DistrictRepository districtRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("districts", districtRepository.findAllByOrderByNameAsc());
        return "districts/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("district", new District());
        return "districts/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("district") District district, BindingResult result) {
        if (result.hasErrors()) {
            return "districts/form";
        }
        districtRepository.save(district);
        return "redirect:/districts";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        District district = districtRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("District not found: " + id));
        model.addAttribute("district", district);
        return "districts/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("district") District district, BindingResult result) {
        if (result.hasErrors()) {
            return "districts/form";
        }
        district.setId(id);
        districtRepository.save(district);
        return "redirect:/districts";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        districtRepository.deleteById(id);
        return "redirect:/districts";
    }

    /**
     * Add a district from a modal on another page (e.g. the order form) without navigating away.
     * Reuses an existing district with the same name (case-insensitively) instead of erroring on
     * the unique constraint, so a slightly different capitalization doesn't create a duplicate.
     */
    @PostMapping(value = "/quick", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickCreate(@RequestParam String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Укажите название района"));
        }
        if (trimmed.length() > 255) {
            return ResponseEntity.badRequest().body(Map.of("error", "Слишком длинное название"));
        }
        District district = districtRepository.findByNameIgnoreCase(trimmed)
                .orElseGet(() -> {
                    District d = new District();
                    d.setName(trimmed);
                    return districtRepository.save(d);
                });
        return ResponseEntity.ok(Map.of("id", district.getId(), "name", district.getName()));
    }
}
