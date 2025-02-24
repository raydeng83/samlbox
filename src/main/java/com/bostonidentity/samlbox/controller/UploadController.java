package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.service.IdpMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
public class UploadController {

    @Autowired
    private IdpMetadataService metadataService;

    @GetMapping("/idp/upload")
    public String uploadPage() {
        return "upload-idp";
    }

    @GetMapping("/sp/upload")
    public String uploadForm() {
        return "upload-sp";
    }

    @PostMapping("/idp/upload")
    public String handleUpload(@RequestParam("file") MultipartFile file) throws IOException {
        try {
            String entityId = metadataService.saveMetadata(file);
            return "redirect:/metadata-summary?entityId=" + entityId;
        } catch (Exception e) {
            return "redirect:/?uploadError=true";
        }
    }
}
