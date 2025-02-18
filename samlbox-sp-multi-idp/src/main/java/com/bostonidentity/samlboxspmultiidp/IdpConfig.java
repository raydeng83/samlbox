package com.bostonidentity.samlboxspmultiidp;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class IdpConfig {
    @Id
    private String entityId;

    // Existing fields
    private String ssoUrl;
    private String signingCertificate;

    // New configuration fields
    private SamlBinding samlBinding = SamlBinding.HTTP_REDIRECT;
    private boolean signRequests = false;
    private String nameIdFormat = "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent";

    public enum SamlBinding {
        HTTP_REDIRECT, HTTP_POST
    }

    // Getters and setters
}
