package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.config.AuthClient;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

@Service
public class MetadataConverterService {

    @Autowired
    private AuthClient authClient;

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.converter.url}")
    private String keycloakConverterUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    public ClientRepresentation convertMetadata(MultipartFile file) throws IOException {
        try {
            String accessToken = authClient.getAccessToken();

            // Read metadata file content
            String metadataContent = new String(file.getBytes());

            // Create JSON payload
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(new MediaType("application", "json", java.nio.charset.StandardCharsets.UTF_8));
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
            headers.setBearerAuth(accessToken);

            // Create HTTP entity
            HttpEntity<String> requestEntity = new HttpEntity<>(metadataContent, headers);

            // Send request to Keycloak
            ResponseEntity<ClientRepresentation> response = restTemplate.exchange(
                    keycloakConverterUrl,
                    HttpMethod.POST,
                    requestEntity,
                    ClientRepresentation.class
            );

            return response.getBody();

        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public ResponseEntity<?> convertAndCreateClient(MultipartFile file) {
        try {
            // 1. Convert metadata to ClientRepresentation
            ClientRepresentation clientRep = convertMetadata(file);

            // 2. Create client using Admin Client
            String createdClientId = createKeycloakClient(clientRep);

            return ResponseEntity.ok(Map.of(
                    "status", "created",
                    "clientId", createdClientId,
                    "message", "SAML client created successfully"
            ));

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "File processing error: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Keycloak operation failed: " + e.getMessage()));
        }
    }

    private String createKeycloakClient(ClientRepresentation clientRep) {
        ClientsResource clientsResource = keycloak
                .realm("master")
                .clients();

        try (Response response = clientsResource.create(clientRep)) {
            if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
                String location = response.getLocation().toString();
                return location.substring(location.lastIndexOf('/') + 1);
            }
            throw new RuntimeException("Failed to create client: " + response.readEntity(String.class));
        }
    }
}
