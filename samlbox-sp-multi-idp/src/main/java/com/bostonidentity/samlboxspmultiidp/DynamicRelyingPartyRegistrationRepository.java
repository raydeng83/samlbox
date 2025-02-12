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
    }

    public void reloadRegistrations() {
        registrations = metadataService.loadMetadataFiles().stream()
                .map(this::parseMetadata)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RelyingPartyRegistration parseMetadata(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            // Create base registration from metadata
            RelyingPartyRegistration baseRegistration = RelyingPartyRegistrations
                    .fromMetadata(inputStream)
                    .registrationId(sanitizeId(path.getFileName().toString()))
                    .entityId(spEntityId)
                    .build();

            // Build asserting party details
//            RelyingPartyRegistration.AssertingPartyDetails assertingPartyDetails =
//                    RelyingPartyRegistration.AssertingPartyDetails.builder()
//                            .entityId(baseRegistration.getAssertingPartyDetails().getEntityId())
//                            .singleSignOnServiceLocation(baseRegistration.getAssertingPartyDetails().getSingleSignOnServiceLocation())
//                            .wantAuthnRequestsSigned(true) // Adjust based on your IDP's requirements
//                            .verificationX509Credentials(c -> c.addAll(
//                                    baseRegistration.getAssertingPartyDetails().getVerificationX509Credentials()
//                            ))
//                            .build();

            // Build the final registration
            return RelyingPartyRegistration
                    .withAssertingPartyMetadata(baseRegistration.getAssertingPartyMetadata())
                    .signingX509Credentials(c -> c.add(signingCredential))
                    .build();
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

