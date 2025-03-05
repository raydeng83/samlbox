package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Base64;
import java.util.List;

@Controller
public class HomeController {
    @Value("{sp.entity-id}")
    private String spEntityId;

    @Autowired
    private DynamicRelyingPartyRegistrationRepository repo;

    @GetMapping("/home")
    public String home() {
        return "home";
    }

    @GetMapping("/saml-box-metadata/sp")
    public String samlBoxMetadataSP() {
        return "redirect:/saml2/metadata/" + Base64.getUrlEncoder().withoutPadding().encodeToString(spEntityId.getBytes());
    }

    @GetMapping("/sso-idp-config")
    public String showIdpConfigPage(Model model, @RequestParam("idpEntityId") String entityId, HttpServletRequest request) {
        model.addAttribute("idpEntityId", entityId);

        String fullUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            fullUrl += "?" + queryString;
        }

        model.addAttribute("fullUrl", fullUrl);

        return "idp-home";
    }

    @GetMapping("/sso-sp-config")
    public String showSpConfigPage(Model model, @RequestParam("spEntityId") String entityId, HttpServletRequest request) {
        model.addAttribute("spEntityId", entityId);

        String fullUrl = request.getRequestURL().toString();
        String queryString = request.getQueryString();
        if (queryString != null) {
            fullUrl += "?" + queryString;
        }

        model.addAttribute("fullUrl", fullUrl);

        return "sp-home";
    }

    @GetMapping("/list")
    public String index(Model model) {
        List<IDPController.IdpInfo> idps = repo.getAllRegistrations().stream()
                .map(reg -> new IDPController.IdpInfo(
                        reg.getRegistrationId(),
                        reg.getAssertingPartyDetails().getEntityId()))
                .toList();
        model.addAttribute("idps", idps);
        return "index";
    }

    @GetMapping("/")
    public String landingPage() {
        return "redirect:/home";
    }

    @GetMapping("/platform-intro")
    public String platformIntro() {
        return "platform-intro";
    }

    @GetMapping("/faq")
    public String faq() {
        return "faq";
    }

    @GetMapping("/guide")
    public String guide() {
        return "guide";
    }
}
