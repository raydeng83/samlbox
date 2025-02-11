package com.bostonidentity.samlbox.config;

// In your configuration class or a dedicated service class (e.g., AuthClient.java)

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class AuthClient {

    @Value("${keycloak.auth-server-url}") // From your application.properties
    private String authServerUrl;

    @Value("${keycloak.realm}") // From your application.properties
    private String realm;

    @Value("${keycloak.client-id}") // From your application.properties
    private String clientId;

    @Value("${keycloak.client-secret}") // From your application.properties
    private String clientSecret;

    @Value("${keycloak.adminUsername}") // From your application.properties
    private String username;

    @Value("${keycloak.adminPassword}") // From your application.properties
    private String password;

    private String accessToken;
    private long tokenExpiry; // Store the expiry time

    private final RestTemplate restTemplate = new RestTemplate();

    public String getAccessToken() {
        if (accessToken != null && System.currentTimeMillis() < tokenExpiry) {
            return accessToken; // Return cached token if still valid
        }

        // 1. Construct the request
        String tokenUrl = String.format("%s/realms/%s/protocol/openid-connect/token", authServerUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "password"); // For service-to-service
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("username", username);
        body.add("password", password);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        // 2. Make the request
        try {
            ResponseEntity<TokenResponse> response = restTemplate.postForEntity(tokenUrl, request, TokenResponse.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                TokenResponse tokenResponse = response.getBody();
                if (tokenResponse != null) {
                    accessToken = tokenResponse.getAccessToken();

                    // Calculate token expiry (Keycloak usually provides an expires_in)
                    long expiresIn = tokenResponse.getExpiresIn();
                    tokenExpiry = System.currentTimeMillis() + (expiresIn * 1000) - 60000; // Subtract 1 minute as buffer
                    return accessToken;
                }
            } else {
                System.err.println("Failed to get token: " + response.getStatusCode());
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions appropriately
        }

        return null; // Return null if token retrieval fails
    }



    // Inner class to represent the token response (important!)
    private static class TokenResponse {
        @JsonProperty("access_token")
        private String access_token;

        @JsonProperty("token_type")
        private String token_type;

        @JsonProperty("expires_in")
        private long expires_in;
        // ... other fields if needed

        public String getAccessToken() {
            return access_token;
        }

        public long getExpiresIn() {
            return expires_in;
        }

        // Getters and setters for other fields
    }
}
