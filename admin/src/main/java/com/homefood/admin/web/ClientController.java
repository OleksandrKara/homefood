package com.homefood.admin.web;

import com.homefood.admin.entity.Client;
import com.homefood.admin.entity.OrderStatus;
import com.homefood.admin.repository.ClientRepository;
import com.homefood.admin.repository.OrderRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientRepository clientRepository;
    private final OrderRepository orderRepository;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("clients", clientRepository.findAllByArchivedFalseOrderByNameAsc());
        model.addAttribute("archivedCount", clientRepository.countByArchivedTrue());
        return "clients/list";
    }

    @GetMapping("/archive")
    public String archive(Model model) {
        model.addAttribute("clients", clientRepository.findAllByArchivedTrueOrderByNameAsc());
        return "clients/archive";
    }

    @PostMapping("/{id}/archive")
    public String archiveClient(@PathVariable Long id, @RequestParam(defaultValue = "/clients") String returnTo) {
        Client client = clientRef(id);
        client.setArchived(true);
        clientRepository.save(client);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @PostMapping("/{id}/unarchive")
    public String unarchiveClient(@PathVariable Long id, @RequestParam(defaultValue = "/clients/archive") String returnTo) {
        Client client = clientRef(id);
        client.setArchived(false);
        clientRepository.save(client);
        return "redirect:" + sanitizeReturnTo(returnTo);
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("addressSuggestions", addressSuggestions());
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
        Client client = clientRef(id);
        model.addAttribute("client", client);
        model.addAttribute("addressSuggestions", addressSuggestions());
        model.addAttribute("orderHistory", orderRepository.findByClientIdOrderByDeliveryDateDescCreatedAtDesc(id));
        model.addAttribute("totalSpent", orderRepository.sumTotalPriceByClientIdAndStatus(id, OrderStatus.DONE));
        return "clients/form";
    }

    private Set<String> addressSuggestions() {
        Set<String> addresses = new LinkedHashSet<>(clientRepository.findDistinctAddresses());
        addresses.addAll(orderRepository.findDistinctDeliveryAddresses());
        return addresses;
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("client") Client client, BindingResult result) {
        if (result.hasErrors()) {
            return "clients/form";
        }
        Client existing = clientRef(id);
        client.setId(id);
        client.setCreatedAt(existing.getCreatedAt());
        client.setArchived(existing.isArchived());
        clientRepository.save(client);
        return "redirect:/clients";
    }

    /**
     * Add/edit a client from a modal on another page (e.g. the order form) without navigating
     * away and losing whatever else is being edited there.
     */
    @PostMapping(value = "/quick", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickCreate(@Valid @ModelAttribute("client") Client client, BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", firstError(result)));
        }
        return ResponseEntity.ok(clientJson(clientRepository.save(client)));
    }

    @PostMapping(value = "/{id}/quick", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> quickUpdate(@PathVariable Long id,
                                                             @Valid @ModelAttribute("client") Client client,
                                                             BindingResult result) {
        if (result.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("error", firstError(result)));
        }
        Client existing = clientRef(id);
        client.setId(id);
        client.setCreatedAt(existing.getCreatedAt());
        client.setArchived(existing.isArchived());
        return ResponseEntity.ok(clientJson(clientRepository.save(client)));
    }

    private Client clientRef(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + id));
    }

    /** Whitelist redirect target so a crafted form field can't send an admin off-app. */
    private String sanitizeReturnTo(String returnTo) {
        return "/clients/archive".equals(returnTo) ? returnTo : "/clients";
    }

    private Map<String, Object> clientJson(Client c) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", c.getId());
        json.put("name", c.getName());
        json.put("phone", c.getPhone() != null ? c.getPhone() : "");
        json.put("address", c.getAddress() != null ? c.getAddress() : "");
        json.put("notes", c.getNotes() != null ? c.getNotes() : "");
        return json;
    }

    private String firstError(BindingResult result) {
        return result.getAllErrors().get(0).getDefaultMessage();
    }
}
