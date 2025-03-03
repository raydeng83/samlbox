package com.bostonidentity.samlbox.api;

import com.bostonidentity.samlbox.service.MetadataConverterService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@Log4j2
public class MetadataConverterController {

    private final MetadataConverterService converterService;

    public MetadataConverterController(MetadataConverterService converterService) {
        this.converterService = converterService;
    }

    @PostMapping(value = "/convert", consumes = "multipart/form-data")
    public ResponseEntity<?> convertMetadata(@RequestParam("file") MultipartFile file, Model model) {
        ResponseEntity<Map<String, String>> responseEntity = converterService.convertAndCreateClient(file);

        try {
            Map<String, String> responseMap =  responseEntity.getBody();
            String clientId = responseMap.get("clientId");

            model.addAttribute("spClientId", clientId);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return converterService.convertAndCreateClient(file);
    }
}
