package com.bostonidentity.samlboxspmultiidp;

import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class SamlExceptionHandler {

    @ExceptionHandler(Saml2AuthenticationException.class)
    public String handleSamlException(Saml2AuthenticationException ex, Model model) {
        model.addAttribute("error", "SAML Processing Error: " + ex.getMessage());
        model.addAttribute("exceptionType", ex.getClass().getSimpleName());
        return "saml-error";
    }

    @ExceptionHandler(InternalAuthenticationServiceException.class)
    public String handleInternalError(InternalAuthenticationServiceException ex, Model model) {
        model.addAttribute("error", "Authentication Service Error: " + ex.getCause().getMessage());
        return "saml-error";
    }
}
