package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.config.AuthClient;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MetadataConverterService {

    @Autowired
    private AuthClient authClient;

    @Autowired
    private Keycloak keycloak;

    @Autowired
    private SpMetadataService spMetadataService;

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

    public ResponseEntity<Map<String,String>> convertAndCreateClient(MultipartFile file) {
        try {
            ClientRepresentation clientRep = convertMetadata(file);
            String createdClientId = createOrUpdateKeycloakClient(clientRep);

            spMetadataService.saveMetadata(file);

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

    private String createOrUpdateKeycloakClient(ClientRepresentation clientRep) {
        RealmResource realmResource = keycloak.realm("master");
        ClientsResource clientsResource = realmResource.clients();

        Map<String, String> attributes = clientRep.getAttributes();
        attributes.put("saml.force.post.binding", "false");
        attributes.put("saml.encrypt", "false");

        // Check if client already exists
        List<ClientRepresentation> existingClients = clientsResource.findByClientId(clientRep.getClientId());

        if (!existingClients.isEmpty()) {
            // Client exists - update it
            String existingClientId = existingClients.get(0).getId();
            ClientResource clientResource = clientsResource.get(existingClientId);

            // Preserve client ID for update
            clientRep.setId(existingClientId);
            clientResource.update(clientRep);

            String clientId = clientRep.getClientId();
            return clientId;
        } else {
            // Create new client
            try (Response response = clientsResource.create(clientRep)) {
                if (response.getStatus() == Response.Status.CREATED.getStatusCode()) {
//                    String location = response.getLocation().toString();
                    return clientRep.getClientId();
                }
                throw new RuntimeException("Failed to create client: " + response.readEntity(String.class));
            }
        }
    }
}
