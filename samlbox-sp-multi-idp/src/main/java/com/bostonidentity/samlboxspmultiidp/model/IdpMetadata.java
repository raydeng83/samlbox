package com.bostonidentity.samlboxspmultiidp.model;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class IdpMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String entityId;
    private String registrationId;
    private String metadataFilePath;
}
