package com.bostonidentity.samlboxspmultiidp.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SessionInfoController {

    @GetMapping("/session-info")
    public String sessionInfo(HttpSession session, Model model,
                              Authentication authentication) {
        // Existing session info
        model.addAttribute("sessionId", session.getId());
        model.addAttribute("creationTime", session.getCreationTime());
        model.addAttribute("lastAccessedTime", session.getLastAccessedTime());
        model.addAttribute("maxInactiveInterval", session.getMaxInactiveInterval());
        model.addAttribute("isNew", session.isNew());
        model.addAttribute("sessionAttributes", session.getAttributeNames());

        // Add user information if authenticated
        if (authentication != null && authentication.isAuthenticated()) {
            model.addAttribute("username", authentication.getName());
            model.addAttribute("authorities", authentication.getAuthorities());
            model.addAttribute("isAuthenticated", true);
        } else {
            model.addAttribute("isAuthenticated", false);
        }

        return "session-info";
    }
}
