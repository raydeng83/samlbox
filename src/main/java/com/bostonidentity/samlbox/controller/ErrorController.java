package com.bostonidentity.samlbox.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class ErrorController {

    @GetMapping("/saml-error")
    public String handleSamlError(HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        model.addAttribute("errorDetails", request.getAttribute("errorDetails"));
        return "saml-error"; // Thymeleaf template name
    }

    @PostMapping("/saml-error")
    public String handleSamlErrorPost(HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        model.addAttribute("errorDetails", request.getAttribute("errorDetails"));
        return "saml-error"; // Thymeleaf template name
    }

    @GetMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        model.addAttribute("errorDetails", request.getAttribute("errorDetails"));
        return "saml-error"; // Thymeleaf template name
    }

    @PostMapping("/error")
    public String handlerrorPost(HttpServletRequest request, Model model) {
        model.addAttribute("errorMessage", request.getAttribute("errorMessage"));
        model.addAttribute("errorDetails", request.getAttribute("errorDetails"));
        return "saml-error"; // Thymeleaf template name
    }
}