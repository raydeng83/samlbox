package com.bostonidentity.samlbox.controller;

import com.bostonidentity.samlbox.service.MetadataConverterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class MetadataConverterController {

    private final MetadataConverterService converterService;

    public MetadataConverterController(MetadataConverterService converterService) {
        this.converterService = converterService;
    }

    @PostMapping(value = "/convert", consumes = "multipart/form-data")
    public ResponseEntity<?> convertMetadata(@RequestParam("file") MultipartFile file) {
        return converterService.convertAndCreateClient(file);
    }
}
