package com.bostonidentity.samlbox.repository;

import com.bostonidentity.samlbox.model.IdpConfig;
import com.bostonidentity.samlbox.model.IdpMetadata;
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
    private final Saml2X509Credential encryptingCredential;
    private final String spEntityId;
    private final String baseUrl;
    private final IdpMetadataRepository idpMetadataRepository;
    private final IdpConfigRepository idpConfigRepository;
    private List<RelyingPartyRegistration> registrations = new ArrayList<>();

    public DynamicRelyingPartyRegistrationRepository(
            Saml2X509Credential signingCredential,
            Saml2X509Credential encryptingCredential,
            String spEntityId,
            IdpMetadataRepository idpMetadataRepository,
            String baseUrl, IdpConfigRepository idpConfigRepository) {
        this.signingCredential = signingCredential;
        this.encryptingCredential = encryptingCredential;
        this.spEntityId = spEntityId;
        this.idpMetadataRepository = idpMetadataRepository;
        this.baseUrl = baseUrl;
        this.idpConfigRepository = idpConfigRepository;

        reloadRegistrations();

        registrations.add(createDefaultRegistration());
    }

    public RelyingPartyRegistration addRegistration(IdpMetadata metadata) {
        RelyingPartyRegistration registration = parseMetadata(metadata, signingCredential, spEntityId);
        registrations.add(registration);

        return registration;
    }

    public List<RelyingPartyRegistration> updateRegistration(IdpConfig idpConfig) {
        Saml2MessageBinding ssoBinding = switch (idpConfig.getSsoBinding()) {
            case "HTTP_REDIRECT" -> Saml2MessageBinding.REDIRECT;
            case "HTTP_POST" -> Saml2MessageBinding.POST;
            default -> Saml2MessageBinding.POST;
        };

        Saml2MessageBinding sloBinding = switch (idpConfig.getSloBinding()) {
            case "HTTP_REDIRECT" -> Saml2MessageBinding.REDIRECT;
            case "HTTP_POST" -> Saml2MessageBinding.POST;
            default -> Saml2MessageBinding.POST;
        };

        RelyingPartyRegistration registration = findByRegistrationId(idpConfig.getRegistrationId())
                .mutate().nameIdFormat(idpConfig.getNameIdFormat())
                .authnRequestsSigned(idpConfig.isSignRequests())
                .assertingPartyMetadata(apd ->
                        apd
                                .wantAuthnRequestsSigned(idpConfig.isSignRequests())
                                .singleSignOnServiceBinding(ssoBinding)
                                .singleSignOnServiceLocation(idpConfig.getSsoLocationUrl())
                                .singleLogoutServiceBinding(sloBinding)
                                .singleLogoutServiceLocation(idpConfig.getSloLocationUrl())
                )
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

        registrations.add(createDefaultRegistration());
    }

    private RelyingPartyRegistration createDefaultRegistration() {
        return RelyingPartyRegistration
                .withRegistrationId(spEntityId)
                .entityId(spEntityId)
                .nameIdFormat("urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified")
                .assertionConsumerServiceLocation(baseUrl + "/login/saml2/sso")
                .singleLogoutServiceLocation(baseUrl + "/logout/saml2/slo")
                .assertingPartyMetadata(apd ->
                        apd
                                .entityId("dummy")
                                .singleSignOnServiceBinding(Saml2MessageBinding.POST)
                                .singleSignOnServiceLocation("dummy")
                                .singleLogoutServiceBinding(Saml2MessageBinding.POST)
                                .singleLogoutServiceLocation("dummy")
                )
                .signingX509Credentials(c -> c.add(signingCredential))
                .decryptionX509Credentials(c -> c.add(encryptingCredential))
                .build();
    }

    private RelyingPartyRegistration parseMetadata(IdpMetadata metadata,
                                                   Saml2X509Credential credential,
                                                   String spEntityId) {
        try (InputStream inputStream = Files.newInputStream(Paths.get(metadata.getMetadataFilePath()))) {

            Optional<IdpConfig> idpConfigOptional = idpConfigRepository.findByRegistrationId(metadata.getRegistrationId());

            if (idpConfigOptional.isPresent()) {
                IdpConfig idpConfig = idpConfigOptional.get();
                Saml2MessageBinding ssoBinding;
                Saml2MessageBinding sloBinding;

                if (idpConfig.getSsoBinding() != null) {
                    ssoBinding = switch (idpConfig.getSsoBinding()) {
                        case "HTTP_REDIRECT" -> Saml2MessageBinding.REDIRECT;
                        case "HTTP_POST" -> Saml2MessageBinding.POST;
                        default -> Saml2MessageBinding.POST;
                    };
                } else {
                    ssoBinding = Saml2MessageBinding.POST;
                }

                if (idpConfig.getSloBinding() != null) {
                    sloBinding = switch (idpConfig.getSloBinding()) {
                        case "HTTP_REDIRECT" -> Saml2MessageBinding.REDIRECT;
                        case "HTTP_POST" -> Saml2MessageBinding.POST;
                        default -> Saml2MessageBinding.POST;
                    };
                } else {
                    sloBinding = Saml2MessageBinding.POST;
                }

                RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                        .fromMetadata(inputStream)
                        .registrationId(metadata.getRegistrationId())
                        .entityId(spEntityId)
                        .nameIdFormat(idpConfig.getNameIdFormat())
                        .authnRequestsSigned(idpConfig.isSignRequests())
                        .assertionConsumerServiceLocation(baseUrl + "/login/saml2/sso")
                        .singleLogoutServiceLocation(baseUrl + "/logout/saml2/slo")
                        .assertingPartyMetadata(apd ->
                                apd
                                        .singleSignOnServiceBinding(ssoBinding)
                                        .singleSignOnServiceLocation(idpConfig.getSsoLocationUrl())
                                        .singleLogoutServiceBinding(sloBinding)
                                        .singleLogoutServiceLocation(idpConfig.getSloLocationUrl())
                        )
                        .signingX509Credentials(c -> c.add(signingCredential))
                        .decryptionX509Credentials(c -> c.add(encryptingCredential))
                        .build();

                return relyingPartyRegistration;
            } else {
                RelyingPartyRegistration relyingPartyRegistration = RelyingPartyRegistrations
                        .fromMetadata(inputStream)
                        .registrationId(metadata.getRegistrationId())
                        .entityId(spEntityId)
                        .assertionConsumerServiceLocation(baseUrl + "/login/saml2/sso")
                        .singleLogoutServiceLocation(baseUrl + "/logout/saml2/slo")
                        .signingX509Credentials(c -> c.add(signingCredential))
                        .decryptionX509Credentials(c -> c.add(encryptingCredential))
                        .build();

                return relyingPartyRegistration;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public RelyingPartyRegistration findByRegistrationId(String id) {

        for (RelyingPartyRegistration r : registrations) {
            if (r.getRegistrationId().equals(id)) {
                return r;
            }
        }

        throw new IllegalArgumentException("Invalid registration ID");
    }

    public List<RelyingPartyRegistration> getAllRegistrations() {
        return Collections.unmodifiableList(registrations);
    }

    public List<IdpMetadata> getAllMetadata() {
        return idpMetadataRepository.findAll();
    }
}

