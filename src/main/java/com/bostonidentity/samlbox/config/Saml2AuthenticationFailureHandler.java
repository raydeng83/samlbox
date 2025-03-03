package com.bostonidentity.samlbox.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;

@Component
@Log4j2
public class Saml2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {

        // Capture SAML response (if available)
        String samlResponse = request.getParameter("SAMLResponse");
        if (samlResponse != null) {
            request.setAttribute("samlResponse", samlResponse);
        }

        // Capture RelayState (if used)
        String relayState = request.getParameter("RelayState");
        request.setAttribute("relayState", relayState);

        // Full stack trace as a string
        StringWriter sw = new StringWriter();
        exception.printStackTrace(new PrintWriter(sw));
        request.setAttribute("stackTrace", sw.toString());

        // Additional context
        request.setAttribute("timestamp", new Date());
        request.setAttribute("sessionId", request.getSession().getId());

        // SAML2-specific errors
        if (exception instanceof Saml2AuthenticationException samlEx) {
            Saml2Error error = samlEx.getSaml2Error();
            request.setAttribute("errorCode", error.getErrorCode());
            request.setAttribute("errorDescription", error.getDescription());
        }

        request.getRequestDispatcher("/saml-error").forward(request, response);
    }
}
