package com.bostonidentity.samlboxspmultiidp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
public class MetadataController {

    private final IdpMetadataService metadataService;
    private final DynamicRelyingPartyRegistrationRepository repo;

    public MetadataController(IdpMetadataService metadataService,
                              DynamicRelyingPartyRegistrationRepository repo) {
        this.metadataService = metadataService;
        this.repo = repo;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<IdpInfo> idps = repo.getAllRegistrations().stream()
                .map(reg -> new IdpInfo(
                        reg.getRegistrationId(),
                        reg.getAssertingPartyDetails().getEntityId()))
                .toList();
        model.addAttribute("idps", idps);
        return "index";
    }

    @GetMapping("/upload")
    public String uploadForm() {
        return "upload";
    }

    @PostMapping("/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            String registrationId = metadataService.saveMetadata(file);
            return "redirect:/metadata-summary/" + registrationId;
        } catch (Exception e) {
            return "redirect:/?uploadError=true";
        }
    }

    @GetMapping("/saml/login")
    public String initiateSamlLogin(@RequestParam("idp") String idpId, HttpServletRequest request) {
        // Redirect to the SAML2 login endpoint for the selected IDP
        return "redirect:/saml2/authenticate/" + idpId;
    }

    @PostMapping("/delete")
    public String deleteIdp(@RequestParam("id") String id) throws IOException {
        metadataService.deleteMetadata(id);
        repo.reloadRegistrations();
        return "redirect:/";
    }

    @GetMapping("/metadata-summary/{registrationId}")
    public String showMetadataSummary(@PathVariable String registrationId, Model model) {
        try {
            RelyingPartyRegistration registration = repo.findByRegistrationId(registrationId);
            IdpMetadataDetails details = metadataService.parseMetadataDetails(registrationId);
            model.addAttribute("metadata", details);
            model.addAttribute("registrationId", registrationId);
            return "metadata-summary";
        } catch (Exception e) {
            return "redirect:/?error=Metadata+parse+failed";
        }
    }

    public record IdpInfo(String id, String entityId) {
    }
}
