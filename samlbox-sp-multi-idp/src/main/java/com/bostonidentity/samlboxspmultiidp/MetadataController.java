package com.bostonidentity.samlboxspmultiidp;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Base64;
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
            String entityId = metadataService.saveMetadata(file);
            return "redirect:/metadata-summary?entityId=" + entityId;
        } catch (Exception e) {
            return "redirect:/?uploadError=true";
        }
    }

    @GetMapping("/saml/login")
    public String initiateSamlLogin(@RequestParam("idp") String idpId, HttpServletRequest request) {

        return "redirect:/saml2/authenticate/" + Base64.getUrlEncoder().withoutPadding().encodeToString(idpId.getBytes());
    }

    @PostMapping("/delete")
    public String deleteIdp(@RequestParam("id") String id) throws IOException {
        metadataService.deleteMetadata(id);
        repo.reloadRegistrations();
        return "redirect:/";
    }

    @GetMapping("/metadata-summary")
    public String showMetadataSummary(@RequestParam("entityId") String entityId, Model model) {
        try {
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());
            RelyingPartyRegistration registration = repo.findByRegistrationId(registrationId);
            IdpMetadataDetails details = IdpMetadataDetails.fromRelyingPartyRegistration(registration);
            model.addAttribute("metadata", details);
            model.addAttribute("registrationId", registrationId);
            model.addAttribute("entityId", details.getEntityId());
            return "metadata-summary";
        } catch (Exception e) {
            return "redirect:/?error=Metadata+parse+failed";
        }
    }

    public record IdpInfo(String id, String entityId) {
    }

    @GetMapping("/view-xml")
    public String viewXmlContent(@RequestParam("entityId") String entityId, Model model) {
        try {
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());
            String xmlContent = metadataService.getFormattedXml(registrationId);
            model.addAttribute("xmlContent", xmlContent);
            model.addAttribute("registrationId", registrationId);
            model.addAttribute("entityId", entityId);
            return "view-xml";
        } catch (Exception e) {
            return "redirect:/?error=XML+not+found";
        }
    }

    @GetMapping("/download-xml")
    public ResponseEntity<Resource> downloadXml(@RequestParam("entityId") String entityId) throws Exception {
        String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

        return metadataService.downloadXml(registrationId);
    }
}
