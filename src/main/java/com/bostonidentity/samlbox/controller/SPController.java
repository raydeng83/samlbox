package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.model.ClientSettings;
import com.bostonidentity.samlbox.service.KeycloakClientService;
import com.bostonidentity.samlbox.service.SpMetadataService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Base64;
import java.util.List;

@Controller
public class SPController {

    private final KeycloakClientService keycloakClientService;
    private final SpMetadataService spMetadataService;

    public SPController(KeycloakClientService keycloakClientService, SpMetadataService spMetadataService) {
        this.keycloakClientService = keycloakClientService;
        this.spMetadataService = spMetadataService;
    }

    @GetMapping("/clients/edit")
    public String showEditForm(@RequestParam String clientId, Model model) {
        model.addAttribute("clientId", clientId);
        model.addAttribute("settings", keycloakClientService.getClientSettings(clientId));
        return "client-settings";
    }

    @PostMapping("/clients/update")
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

    @GetMapping("/view-sp-xml")
    public String viewXmlContent(@RequestParam("entityId") String entityId, Model model) {
        try {
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());
            String xmlContent = spMetadataService.getFormattedXml(registrationId);
            model.addAttribute("xmlContent", xmlContent);
            model.addAttribute("registrationId", registrationId);
            model.addAttribute("entityId", entityId);
            return "view-sp-xml";
        } catch (Exception e) {
            return "redirect:/?error=XML+not+found";
        }
    }

    @GetMapping("/download-sp-xml")
    public ResponseEntity<Resource> downloadXml(@RequestParam("entityId") String entityId) throws Exception {
        String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

        return spMetadataService.downloadXml(registrationId);
    }
}
