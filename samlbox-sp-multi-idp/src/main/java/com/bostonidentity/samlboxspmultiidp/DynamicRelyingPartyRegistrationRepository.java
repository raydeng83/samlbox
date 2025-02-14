package com.bostonidentity.samlboxspmultiidp;

import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// DynamicRelyingPartyRegistrationRepository.java
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final IdpMetadataService metadataService;
    private final Saml2X509Credential signingCredential;
    private final String spEntityId;
    private List<RelyingPartyRegistration> registrations = new ArrayList<>();

    public DynamicRelyingPartyRegistrationRepository(IdpMetadataService metadataService,
                                                     Saml2X509Credential signingCredential, String spEntityId) {
        this.metadataService = metadataService;
        this.signingCredential = signingCredential;
        this.spEntityId = spEntityId;
        reloadRegistrations();

        if (registrations.isEmpty()) {
            registrations.add(createDefaultRegistration());
        }
    }

    private RelyingPartyRegistration createDefaultRegistration() {
        return RelyingPartyRegistration.withRegistrationId("default-idp")
                .entityId(spEntityId)
                .assertingPartyMetadata(party -> party
                        .entityId("https://default-idp.example.com")
                        .singleSignOnServiceLocation("https://default-idp.example.com/sso")
                        .wantAuthnRequestsSigned(true)
                )
                .signingX509Credentials(c -> c.add(signingCredential))
                .build();
    }

    public void reloadRegistrations() {
        registrations = metadataService.loadMetadataFiles().stream()
                .map(this::parseMetadata)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RelyingPartyRegistration parseMetadata(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {

            RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                    .fromMetadata(inputStream)
                    .registrationId(spEntityId)
                    .entityId(spEntityId)
                    .signingX509Credentials(c -> c.add(signingCredential))
                    .build();

            return relyingPartyRegistration;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String sanitizeId(String fileName) {
        return fileName.replaceAll("[^a-zA-Z0-9-]", "_");
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String id) {
        return registrations.stream()
                .filter(r -> r.getRegistrationId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid registration ID"));
    }

    public List<RelyingPartyRegistration> getAllRegistrations() {
        return Collections.unmodifiableList(registrations);
    }
}

