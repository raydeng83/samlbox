package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.service.KeycloakClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final KeycloakClientService keycloakClientService;

    public ClientController(KeycloakClientService keycloakClientService) {
        this.keycloakClientService = keycloakClientService;
    }

    @GetMapping("/edit-redirect-uris")
    public String showEditForm(@RequestParam String clientId, Model model) {
        List<String> currentUris = keycloakClientService.getRedirectUris(clientId);
        model.addAttribute("clientId", clientId);
        model.addAttribute("redirectUris", currentUris);
        return "edit-redirect-uris";
    }

    @PostMapping("/update-redirect-uris")
    public String updateRedirectUris(
            @RequestParam String clientId,
            @RequestParam List<String> redirectUris) {
        keycloakClientService.updateRedirectUris(clientId, redirectUris);
        return "redirect:/clients/edit-redirect-uris?clientId=" + clientId + "&success=true";
    }
}
