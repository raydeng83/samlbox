package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.config.AuthClient;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

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
}
