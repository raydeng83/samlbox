package com.bostonidentity.samlboxspmultiidp;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SamlErrorController implements ErrorController {

    private static final Logger logger = LoggerFactory.getLogger(SamlErrorController.class);


    @GetMapping("/saml-error")
    public String handleSamlError(HttpServletRequest request, Model model) {
        // Extract error details from request attributes
        Object error = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
        Object exceptionType = request.getAttribute(RequestDispatcher.ERROR_EXCEPTION_TYPE);

        // Log the error
        logger.error("SAML SSO Error: {}", error);

        // Add details to model
        model.addAttribute("error", error);
        model.addAttribute("exceptionType", exceptionType);

        return "saml-error"; // Thymeleaf template name
    }
}