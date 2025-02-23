package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.config.AuthClient;
import com.bostonidentity.samlbox.model.ClientSettings;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KeycloakClientService {
    @Autowired
    private AuthClient authClient;

    @Autowired
    private Keycloak keycloak;

    @Value("${keycloak.realm}")
    private String realm;

    public void updateRedirectUris(String clientIdToUpdate, List<String> redirectUris) {
        RealmResource realmResource = keycloak.realm(realm);
        ClientsResource clientsResource = realmResource.clients();

        // Find client by clientId
        List<ClientRepresentation> clients = clientsResource.findByClientId(clientIdToUpdate);
        if (clients.isEmpty()) {
            throw new RuntimeException("Client not found: " + clientIdToUpdate);
        }

        // Update client configuration
        ClientRepresentation clientRep = clients.get(0);
        clientRep.setRedirectUris(redirectUris);
        clientsResource.get(clientRep.getId()).update(clientRep);
    }

    public List<String> getRedirectUris(String clientIdToUpdate) {
        RealmResource realmResource = keycloak.realm(realm);
        ClientsResource clientsResource = realmResource.clients();

        List<ClientRepresentation> clients = clientsResource.findByClientId(clientIdToUpdate);
        if (clients.isEmpty()) {
            throw new RuntimeException("Client not found: " + clientIdToUpdate);
        }

        return clients.get(0).getRedirectUris();
    }

    public ClientSettings getClientSettings(String clientId) {
        RealmResource realmResource = keycloak.realm(realm);

        ClientRepresentation client = realmResource.clients().findByClientId(clientId).get(0);

        Map<String, String> attrs = client.getAttributes() != null ?
                client.getAttributes() : new HashMap<>();

        ClientSettings settings = new ClientSettings();
        settings.setIdpInitiatedSsoUrl(attrs.get("saml_idp_initiated_sso_url_name"));
        settings.setAcsUrlPost(attrs.get("saml_assertion_consumer_url_post"));
        settings.setAcsUrlRedirect(attrs.get("saml_assertion_consumer_url_redirect"));
        settings.setNameIdFormat(attrs.get("saml_name_id_format"));
        settings.setLogoutUrlPost(attrs.get("saml_single_logout_service_url_post"));
        settings.setLogoutUrlRedirect(attrs.get("saml_single_logout_service_url_redirect"));
        settings.setSignatureCert(attrs.get("saml.signing.certificate"));
        settings.setEncryptionCert(attrs.get("saml.encryption.certificate"));

        settings.setClientSignatureRequired(
                Boolean.parseBoolean(attrs.getOrDefault("saml.client.signature", "false"))
        );
        settings.setEncryptAssertions(
                Boolean.parseBoolean(attrs.getOrDefault("saml.encrypt", "false"))
        );

        settings.setSignResponse(
                Boolean.parseBoolean(attrs.getOrDefault("saml.server.signature", "false"))
        );
        settings.setSignAssertions(
                Boolean.parseBoolean(attrs.getOrDefault("saml.assertion.signature", "false"))
        );
        settings.setSignatureAlgorithm(
                attrs.getOrDefault("saml.signature.algorithm", "RSA_SHA256")
        );

        return settings;
    }

    public boolean updateClientSettings(String clientId, ClientSettings settings) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            ClientResource clientRes = realmResource.clients().get(
                    realmResource.clients().findByClientId(clientId).get(0).getId()
            );

            ClientRepresentation client = clientRes.toRepresentation();
            Map<String, String> attrs = client.getAttributes() != null ?
                    client.getAttributes() : new HashMap<>();

            // Update SAML attributes
            attrs.put("saml_idp_initiated_sso_url_name", settings.getIdpInitiatedSsoUrl());
            attrs.put("saml_assertion_consumer_url_post", settings.getAcsUrlPost());
            attrs.put("saml_assertion_consumer_url_redirect", settings.getAcsUrlRedirect());
            attrs.put("saml_name_id_format", settings.getNameIdFormat());
            attrs.put("saml_single_logout_service_url_post", settings.getLogoutUrlPost());
            attrs.put("saml_single_logout_service_url_redirect", settings.getLogoutUrlRedirect());

            // Update valid redirect URIs
            List<String> validRedirectUris = new ArrayList<>();
            if (settings.getAcsUrlPost() != null && !settings.getAcsUrlPost().isEmpty()) {
                validRedirectUris.add(settings.getAcsUrlPost());
            }
            if (settings.getAcsUrlRedirect() != null && !settings.getAcsUrlRedirect().isEmpty()) {
                validRedirectUris.add(settings.getAcsUrlRedirect());
            }
            client.setRedirectUris(validRedirectUris); // Set only ACS URLs as valid redirect URIs

            // Add certificate attributes
            if (settings.getSignatureCert() != null && !settings.getSignatureCert().isEmpty()) {
                attrs.put("saml.signing.certificate",
                        cleanCertificate(settings.getSignatureCert()));
            }

            if (settings.getEncryptionCert() != null && !settings.getEncryptionCert().isEmpty()) {
                attrs.put("saml.encryption.certificate",
                        cleanCertificate(settings.getEncryptionCert()));
            }

            attrs.put("saml.client.signature",
                    String.valueOf(settings.isClientSignatureRequired()));
            attrs.put("saml.encrypt",
                    String.valueOf(settings.isEncryptAssertions()));

            attrs.put("saml.server.signature",
                    String.valueOf(settings.isSignResponse()));
            attrs.put("saml.assertion.signature",
                    String.valueOf(settings.isSignAssertions()));
            attrs.put("saml.signature.algorithm",
                    settings.getSignatureAlgorithm());

            client.setAttributes(attrs);
            clientRes.update(client);
            return true; // Success
        } catch (Exception e) {
            return false; // Failure
        }
    }

    private String cleanCertificate(String cert) {
        // Remove BEGIN/END CERTIFICATE lines and whitespace
        return cert.replaceAll("-----BEGIN CERTIFICATE-----", "")
                .replaceAll("-----END CERTIFICATE-----", "")
                .replaceAll("\\s+", "");
    }
}
