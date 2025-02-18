package com.bostonidentity.samlboxspmultiidp;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Controller
public class IdpConfigController {

    @Autowired
    private IdpConfigRepository idpRepository;

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
            @ModelAttribute IdpConfig updatedConfig
    ) {
        IdpConfig existing = idpRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("IDP not found"));

        existing.setSamlBinding(updatedConfig.getSamlBinding());
        existing.setSignRequests(updatedConfig.isSignRequests());
        existing.setNameIdFormat(updatedConfig.getNameIdFormat());

        idpRepository.save(existing);
        return "redirect:/";
    }
}
