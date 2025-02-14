package com.bostonidentity.samlboxspmultiidp;


import jakarta.persistence.*;

@Entity
public class IdpMetadata {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(nullable = false)
    private Long id;

    private String entityId;
    private String registrationId;
    private String metadataFilePath;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityId() { return entityId; }
    public void setEntityId(String entityId) { this.entityId = entityId; }

    public String getRegistrationId() { return registrationId; }
    public void setRegistrationId(String registrationId) { this.registrationId = registrationId; }

    public String getMetadataFilePath() { return metadataFilePath; }
    public void setMetadataFilePath(String metadataFilePath) { this.metadataFilePath = metadataFilePath; }
}
