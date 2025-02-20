package com.bostonidentity.samlboxspmultiidp.controller;

import com.bostonidentity.samlboxspmultiidp.repository.IdpConfigRepository;
import com.bostonidentity.samlboxspmultiidp.service.IdpConfigService;
import com.bostonidentity.samlboxspmultiidp.model.IdpConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@Controller
public class IdpConfigController {

    @Autowired
    private IdpConfigRepository idpRepository;
    @Autowired
    private IdpConfigService idpConfigService;

    @GetMapping("/idp/configure")
    public String configPage(@RequestParam("entityId") String entityId, Model model) {
        IdpConfig idpConfig = idpRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("IDP not found"));

        List<String> bindingList = Arrays.asList("HTTP_POST", "HTTP_REDIRECT");
        List<String> nameIdFormatList = Arrays.asList(
                "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified",
                "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
                "urn:oasis:names:tc:SAML:2.0:nameid-format:transient",
                "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
                "urn:oasis:names:tc:SAML:2.0:nameid-format:kerberos",
                "urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName"
        );

        model.addAttribute("entityId", entityId);
        model.addAttribute("idpConfig", idpConfig);
        model.addAttribute("bindingList", bindingList);
        model.addAttribute("nameIdFormatList", nameIdFormatList);

        return "idp-config";
    }

    @PostMapping("/idp/configure")
    public String saveConfig(
            @RequestParam("entityId") String entityId,
            @ModelAttribute IdpConfig idpConfig
    ) {
        idpConfigService.updateIdpConfig(idpConfig);
        return "redirect:/";
    }
}
