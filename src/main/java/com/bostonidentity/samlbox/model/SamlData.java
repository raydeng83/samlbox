package com.bostonidentity.samlbox.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SamlData {
    private String issuer;
    private String nameId;
    private String audience;
    private String validUntil;

    // Getters and setters
}
