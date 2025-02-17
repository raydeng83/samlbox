package com.bostonidentity.samlboxspmultiidp;


import lombok.Getter;
import lombok.Setter;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class IdpMetadataDetails {

    private String entityId;
    private boolean wantAuthnRequestsSigned;
    private List<String> signingCertificates;
    private List<String> encryptionCertificates;
    private List<ServiceEndpoint> singleSignOnServices;
    private List<ServiceEndpoint> singleLogoutServices;
    private List<String> nameIdFormats;

    public static IdpMetadataDetails fromRelyingPartyRegistration(RelyingPartyRegistration registration) {
        IdpMetadataDetails details = new IdpMetadataDetails();
        details.setEntityId(registration.getAssertingPartyDetails().getEntityId());
        details.setWantAuthnRequestsSigned(registration.getAssertingPartyDetails().getWantAuthnRequestsSigned());

        // Extract signing and encryption certificates
        details.setSigningCertificates(registration.getAssertingPartyDetails().getVerificationX509Credentials()
                .stream()
                .map(credential -> credential.getCertificate().toString())
                .collect(Collectors.toList()));

        details.setEncryptionCertificates(registration.getAssertingPartyDetails().getEncryptionX509Credentials()
                .stream()
                .map(credential -> credential.getCertificate().toString())
                .collect(Collectors.toList()));

        // Extract SSO and SLO endpoints
        details.setSingleSignOnServices(List.of(
                new ServiceEndpoint(
                        registration.getAssertingPartyDetails().getSingleSignOnServiceLocation(),
                        registration.getAssertingPartyDetails().getSingleSignOnServiceBinding().name()
                )
        ));

        details.setSingleLogoutServices(List.of(
                new ServiceEndpoint(
                        registration.getAssertingPartyDetails().getSingleLogoutServiceLocation(),
                        registration.getAssertingPartyDetails().getSingleLogoutServiceBinding().name()
                )
        ));

        // Extract NameID formats
//        details.setNameIdFormats();

        return details;
    }

    @Getter @Setter
    public static class ServiceEndpoint {
        private String location;
        private String binding;

        public ServiceEndpoint(String location, String binding) {
            this.location = location;
            this.binding = binding;
        }

        // Getters and setters
    }

    // Getters and setters for all fields
}
