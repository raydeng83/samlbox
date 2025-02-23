package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.model.SamlResponseDetails;
import com.bostonidentity.samlbox.service.SamlResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class SamlResponseController {

    private static final Logger logger = LoggerFactory.getLogger(SamlResponseController.class);


    public SamlResponseController(
    ) {
    }

    @GetMapping("/saml-response")
    public String showResponse(Model model,
                               @RequestParam(name = "samlResponse", required = false) String samlResponse,
                               @RequestParam(name = "relayState", required = false) String relayState
    ) {

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
