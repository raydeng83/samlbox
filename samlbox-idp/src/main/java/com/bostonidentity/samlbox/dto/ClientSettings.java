package com.bostonidentity.samlbox.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ClientSettings {
    private String idpInitiatedSsoUrl;
    private String acsUrlPost;
    private String acsUrlRedirect;
    private String nameIdFormat;
    private String logoutUrlPost;
    private String logoutUrlRedirect;

    private String signatureCert;  // PEM format
    private String encryptionCert; // PEM format

    private boolean clientSignatureRequired;
    private boolean encryptAssertions;

    private boolean signResponse;
    private boolean signAssertions;
    private String signatureAlgorithm;

    // Getters and Setters
}
