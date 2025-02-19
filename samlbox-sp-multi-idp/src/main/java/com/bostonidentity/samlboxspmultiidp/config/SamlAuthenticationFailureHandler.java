package com.bostonidentity.samlboxspmultiidp.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;

@Component
public class SamlAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // Handle different exception types
        String errorMessage = "Authentication failed";

        if (exception instanceof Saml2AuthenticationException) {
            errorMessage = "SAML processing error: " + exception.getMessage();
        } else if (exception instanceof InternalAuthenticationServiceException) {
            errorMessage = "Internal server error during authentication";
        }

        // Redirect to error endpoint with message
        getRedirectStrategy().sendRedirect(request, response,
                "/saml-error?error=" + URLEncoder.encode(errorMessage, "UTF-8"));
    }
}
