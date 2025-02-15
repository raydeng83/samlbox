package com.bostonidentity.samlboxspmultiidp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.HashMap;
import java.util.Optional;

@Controller
public class SamlResponseController {

    private static final Logger logger = LoggerFactory.getLogger(SamlResponseController.class);

    private final Saml2AuthenticationTokenConverter tokenConverter;

    public SamlResponseController(
            Saml2AuthenticationTokenConverter tokenConverter) {
        this.tokenConverter = tokenConverter;
    }

    @GetMapping("/saml-response")
    public String showResponse(Model model,
                               @RequestParam(name = "samlResponse", required = false) String samlResponse,
                               @RequestParam(name = "relayState", required = false) String relayState,
                               @RequestParam(name = "registrationId", required = false) String registrationId) {

        logger.info("SAML Response: {}", samlResponse);
        logger.info("Relay State: {}", relayState);

        if (samlResponse != null) {
            try {
                SamlResponseDetails samlDetails = SamlResponseProcessor.processSamlResponse(samlResponse);

                model.addAttribute("samlDetails", samlDetails);
            } catch (Exception e) {
                model.addAttribute("error", "Failed to process SAML Response: " + e.getMessage());
            }
        } else {
            model.addAttribute("error", "No SAML response found.");
        }

        return "saml-response";
    }
}
