package com.bostonidentity.samlbox.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

@ControllerAdvice
public class Saml2GlobalExceptionHandler {

    // Catch SAML2-specific exceptions
    @ExceptionHandler({
            Exception.class // SAML2 auth failures
    })
    public String handleAllExceptions(Exception ex, HttpServletRequest request) {
        // Capture SAMLResponse from request (if available)
        String samlResponse = request.getParameter("SAMLResponse");
        request.setAttribute("samlResponse", samlResponse);

        // Stack trace
        StringWriter sw = new StringWriter();
        ex.printStackTrace(new PrintWriter(sw));
        request.setAttribute("stackTrace", sw.toString());

        // Contextual data
        request.setAttribute("timestamp", new Date());
        request.setAttribute("errorMessage", ex.getMessage());
        request.setAttribute("errorDetails", ex.getClass().getName());

        return "forward:/saml-error";
    }
}
