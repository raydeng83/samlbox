package com.bostonidentity.samlbox.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SamlResponseRedirectHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        // Extract SAML response, relay state, and registration ID
        String samlResponse = request.getParameter("SAMLResponse");
        String relayState = request.getParameter("RelayState");

        Saml2AuthenticatedPrincipal saml2AuthenticatedPrincipal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

        String registrationId = saml2AuthenticatedPrincipal.getRelyingPartyRegistrationId();

        // URL-encode the SAML response
        String encodedSamlResponse = URLEncoder.encode(samlResponse, StandardCharsets.UTF_8.toString());

        // Redirect to the SAML response page with the encoded SAML response, relay state, and registration ID
        String redirectUrl = "/saml-response?samlResponse=" + encodedSamlResponse
                + "&relayState=" + relayState;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
