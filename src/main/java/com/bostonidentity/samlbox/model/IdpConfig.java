package com.bostonidentity.samlbox.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class IdpConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String entityId;
    private String registrationId;

    private String ssoBinding;
    private String ssoLocationUrl;

    private String sloBinding;
    private String sloLocationUrl;

    private String signingCertificate;

    private boolean signRequests = false;
    private String nameIdFormat = "urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified";

    // Getters and setters
}
