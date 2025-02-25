package com.bostonidentity.samlbox.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Base64;

@Controller
public class HomeController {
    @Value("{sp.entity-id}")
    private String spEntityId;

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/samlbox-metadata/sp")
    public String samlBoxMetadataSP() {
        return "redirect:/saml2/metadata/" + Base64.getUrlEncoder().withoutPadding().encodeToString(spEntityId.getBytes());
    }

    @GetMapping("/sso-config")
    public String showConfigPage(Model model, @RequestParam("idpEntityId") String entityId) {
        model.addAttribute("idpEntityId", entityId);

        return "idp-home";
    }
}
