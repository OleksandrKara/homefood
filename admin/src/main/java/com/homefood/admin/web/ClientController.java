package com.homefood.admin.web;

import com.homefood.admin.entity.Client;
import com.homefood.admin.repository.ClientRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientRepository.findAllByOrderByNameAsc());
        return "clients/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        return "clients/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("client") Client client, BindingResult result) {
        if (result.hasErrors()) {
            return "clients/form";
        }
        clientRepository.save(client);
        return "redirect:/clients";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        model.addAttribute("client", client);
        return "clients/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("client") Client client, BindingResult result) {
        if (result.hasErrors()) {
            return "clients/form";
        }
        client.setId(id);
        Client existing = clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
        client.setCreatedAt(existing.getCreatedAt());
        clientRepository.save(client);
        return "redirect:/clients";
    }
}
