package com.bostonidentity.samlboxspmultiidp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
                // Decode the SAML response
//                String decodedSamlResponse = URLDecoder.decode(samlResponse);
//                Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) authentication.getPrincipal();

                // Add attributes to the model
//                model.addAttribute("attributes", principal.getAttributes());
//                model.addAttribute("name", principal.getName());
                    model.addAttribute("rawResponse", samlResponse);
//                model.addAttribute("error", "Failed to parse SAML response.");
            } catch (Exception e) {
                model.addAttribute("error", "Invalid SAML response: " + e.getMessage());
            }
        } else {
            model.addAttribute("error", "No SAML response found.");
        }

        return "saml-response";
    }
}
