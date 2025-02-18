package com.bostonidentity.samlboxspmultiidp;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class IdpConfigController {

    @Autowired
    private IdpConfigRepository idpRepository;

    @GetMapping("/idp/configure")
    public String configPage(@RequestParam("entityId") String entityId, Model model) {
        IdpConfig idp = idpRepository.findByEntityId(entityId)
                .orElseThrow(() -> new IllegalArgumentException("IDP not found"));
        model.addAttribute("idp", idp);
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
