package com.bostonidentity.samlbox.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SessionInfoController {

    @GetMapping("/session-info")
    public String sessionInfo(@RequestParam("entityId") String entityId, HttpSession session, Model model,
                              Authentication authentication) {
        model.addAttribute("entityId", entityId);

        // Existing session info
        model.addAttribute("creationTime", session.getLastAccessedTime());

        // Add user information if authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("isAuthenticated", true);
            model.addAttribute("username", authentication.getName());
            Saml2AuthenticatedPrincipal samlPrincipal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

            model.addAttribute("attributes", samlPrincipal.getAttributes());
            model.addAttribute("sessionIndexes", samlPrincipal.getSessionIndexes());
            model.addAttribute("registrationId", samlPrincipal.getRelyingPartyRegistrationId());
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "session-info";
    }
}
