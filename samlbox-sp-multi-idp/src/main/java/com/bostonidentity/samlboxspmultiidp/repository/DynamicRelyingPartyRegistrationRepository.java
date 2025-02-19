package com.bostonidentity.samlboxspmultiidp.repository;

import com.bostonidentity.samlboxspmultiidp.config.IdpConfig;
import com.bostonidentity.samlboxspmultiidp.model.IdpMetadata;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.InputStream;
import java.nio.file.Files;
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

    public RelyingPartyRegistration addRegistration(IdpMetadata metadata) {
        RelyingPartyRegistration registration = parseMetadata(metadata, signingCredential, spEntityId);
        registrations.add(registration);

        return registration;
    }

    public RelyingPartyRegistration updateRegistration(IdpMetadata metadata) {
        RelyingPartyRegistration registration = parseMetadata(metadata, signingCredential, spEntityId);

        for (int i = 0; i < registrations.size(); i++) {
            if (registration != null && registrations.get(i).getRegistrationId().equals(registration.getRegistrationId())) {
                registrations.set(i, registration);
                break; // Stop after the first replacement
            }
        }

        return registration;
    }

    public List<RelyingPartyRegistration> updateRegistration(IdpConfig idpConfig) {
        Saml2MessageBinding binding = switch (idpConfig.getSsoBinding()) {
            case "HTTP_REDIRECT" -> Saml2MessageBinding.REDIRECT;
            case "HTTP_POST" -> Saml2MessageBinding.POST;
            default -> Saml2MessageBinding.POST;
        };

        RelyingPartyRegistration registration = findByRegistrationId(idpConfig.getRegistrationId())
                .mutate().nameIdFormat(idpConfig.getNameIdFormat())
                .authnRequestsSigned(idpConfig.isSignRequests())
                .assertingPartyMetadata(apd ->
                        apd.singleSignOnServiceBinding(binding).singleSignOnServiceLocation(idpConfig.getSsoLocationUrl()))
                .build();

        for (int i = 0; i < registrations.size(); i++) {
            if (registration != null && registrations.get(i).getRegistrationId().equals(registration.getRegistrationId())) {
                registrations.set(i, registration);
                break;
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
//                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso/" + metadata.getRegistrationId())
                    .assertionConsumerServiceLocation("{baseUrl}/login/saml2/sso" )
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
//        return registrations.stream()
//                .filter(r -> r.getRegistrationId().equals(id))
//                .findFirst()
//                .orElseThrow(() -> new IllegalArgumentException("Invalid registration ID"));

        // Step 1: Iterate through the list of registrations
        for (RelyingPartyRegistration r : registrations) {
            // Step 2: Check if the registration ID matches the given ID
            if (r.getRegistrationId().equals(id)) {
                // Step 3: Return the first matching registration
                return r;
            }
        }

// Step 4: Throw an exception if no matching registration is found
        throw new IllegalArgumentException("Invalid registration ID");
    }

    public List<RelyingPartyRegistration> getAllRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public List<IdpMetadata> getAllMetadata() {
        return idpMetadataRepository.findAll();
    }
}

