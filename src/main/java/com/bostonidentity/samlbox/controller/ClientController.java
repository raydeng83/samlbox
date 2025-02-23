package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.model.ClientSettings;
import com.bostonidentity.samlbox.service.KeycloakClientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
public class ClientController {

    private final KeycloakClientService keycloakClientService;

    public ClientController(KeycloakClientService keycloakClientService) {
        this.keycloakClientService = keycloakClientService;
    }

//    @GetMapping("/edit-redirect-uris")
//    public String showEditForm(@RequestParam String clientId, Model model) {
//        List<String> currentUris = keycloakClientService.getRedirectUris(clientId);
//        model.addAttribute("clientId", clientId);
//        model.addAttribute("redirectUris", currentUris);
//        return "edit-redirect-uris";
//    }

    @PostMapping("/update-redirect-uris")
    public String updateRedirectUris(
            @RequestParam String clientId,
            @RequestParam List<String> redirectUris) {
        keycloakClientService.updateRedirectUris(clientId, redirectUris);
        return "redirect:/clients/edit-redirect-uris?clientId=" + clientId + "&success=true";
    }


    @GetMapping("/edit")
    public String showEditForm(@RequestParam String clientId, Model model) {
        model.addAttribute("clientId", clientId);
        model.addAttribute("settings", keycloakClientService.getClientSettings(clientId));
        return "client-settings";
    }

    @PostMapping("/update")
    public String updateSettings(
            @RequestParam String clientId,
            @ModelAttribute ClientSettings settings,
            RedirectAttributes redirectAttributes
    ) {
        boolean success = keycloakClientService.updateClientSettings(clientId, settings);
        if (success) {
            redirectAttributes.addFlashAttribute("message", "Settings updated successfully!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } else {
            redirectAttributes.addFlashAttribute("message", "Failed to update settings. Please try again.");
            redirectAttributes.addFlashAttribute("messageType", "danger");
        }
        return "redirect:/clients/edit?clientId=" + clientId;
    }
}
