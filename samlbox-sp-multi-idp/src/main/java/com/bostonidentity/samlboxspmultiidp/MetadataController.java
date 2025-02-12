package com.bostonidentity.samlboxspmultiidp;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
        metadataService.saveMetadata(file.getOriginalFilename(), file.getInputStream());
        repo.reloadRegistrations();
        return "redirect:/";
    }

    public record IdpInfo(String id, String entityId) {}
}
