package com.bostonidentity.samlbox.config;

import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlbox.service.SamlUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@Log4j2
public class EntityIdRelyingPartyRegistrationResolver implements RelyingPartyRegistrationResolver {

    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    public EntityIdRelyingPartyRegistrationResolver(DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository) {
        this.dynamicRelyingPartyRegistrationRepository = dynamicRelyingPartyRegistrationRepository;
    }

    @Override
    public RelyingPartyRegistration resolve(HttpServletRequest request, String registrationId) {
        String issuerEntityId = extractIssuerEntityId(request);
        registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(issuerEntityId.getBytes());
        RelyingPartyRegistration registration = dynamicRelyingPartyRegistrationRepository.findUniqueByAssertingPartyEntityId(registrationId);

        if (registration == null) {
            throw new RuntimeException("Can't resolve SAMl registration, registration ID not found.");
        }

        return registration;
    }

    private String extractIssuerEntityId(HttpServletRequest request) {
        String issuerEntityId = SamlUtils.extractIssuerEntityId(request);
        return issuerEntityId;
    }
}
