package com.bostonidentity.samlbox.api;

import com.bostonidentity.samlbox.service.KeycloakClientService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientAPI {

    private final KeycloakClientService keycloakClientService;

    public ClientAPI(KeycloakClientService keycloakClientService) {
        this.keycloakClientService = keycloakClientService;
    }

    @PostMapping("/update-redirect-uris")
    public ResponseEntity<String> updateRedirectUris(
            @RequestParam String clientId,
            @RequestBody List<String> redirectUris) {
        try {
            keycloakClientService.updateRedirectUris(clientId, redirectUris);
            return ResponseEntity.ok("Redirect URIs updated successfully.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to update redirect URIs: " + e.getMessage());
        }
    }
}
