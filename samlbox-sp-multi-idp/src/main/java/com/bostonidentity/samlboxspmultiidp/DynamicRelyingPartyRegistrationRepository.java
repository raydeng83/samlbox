package com.bostonidentity.samlboxspmultiidp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

// DynamicRelyingPartyRegistrationRepository.java
public class DynamicRelyingPartyRegistrationRepository implements RelyingPartyRegistrationRepository {

    private final Saml2X509Credential signingCredential;
    private final String spEntityId;
    private final IdpMetadataRepository idpMetadataRepository;
    private List<RelyingPartyRegistration> registrations = new ArrayList<>();

    public DynamicRelyingPartyRegistrationRepository(
                                                     Saml2X509Credential signingCredential, String spEntityId, IdpMetadataRepository idpMetadataRepository) {
        this.signingCredential = signingCredential;
        this.spEntityId = spEntityId;
        this.idpMetadataRepository = idpMetadataRepository;

        reloadRegistrations();

        registrations.add(createDefaultRegistration());
    }

    public List<RelyingPartyRegistration> addRegistration(IdpMetadata metadata) {
        RelyingPartyRegistration registration = parseMetadata(metadata, signingCredential,spEntityId);
        registrations.add(registration);

        return registrations;
    }

    public List<RelyingPartyRegistration> updateRegistration(IdpMetadata metadata) {
        RelyingPartyRegistration registration = parseMetadata(metadata, signingCredential,spEntityId);

        for (int i = 0; i < registrations.size(); i++) {
            if (registration != null && registrations.get(i).getRegistrationId().equals(registration.getRegistrationId())) {
                registrations.set(i, registration);
                break; // Stop after the first replacement
            }
        }

        return registrations;
    }

    public void reloadRegistrations() {
        // Load all metadata from the database and parse them into RelyingPartyRegistration objects
        this.registrations = getAllMetadata().stream()
                .map(metadata -> parseMetadata(metadata, signingCredential, spEntityId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
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

    private RelyingPartyRegistration parseMetadata(IdpMetadata metadata,
                                                   Saml2X509Credential credential,
                                                   String spEntityId) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(metadata.getMetadataFilePath()))) {

            RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                    .fromMetadata(inputStream)
                    .registrationId(metadata.getRegistrationId())
                    .entityId(spEntityId)
//                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/" + spEntityId)
                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/" + metadata.getRegistrationId())
                    .signingX509Credentials(c -> c.add(signingCredential))
                    .build();

            return relyingPartyRegistration;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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

    public List<IdpMetadata> getAllMetadata() {
        return idpMetadataRepository.findAll();
    }
}

