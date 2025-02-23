package com.bostonidentity.samlbox.config;

import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlbox.service.SamlUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.RelyingPartyRegistrationResolver;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class EntityIdRelyingPartyRegistrationResolver implements RelyingPartyRegistrationResolver {

    @Autowired
    private DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    @Override
    public RelyingPartyRegistration resolve(HttpServletRequest request, String registrationId) {
        String issuerEntityId = extractIssuerEntityId(request);
        registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(issuerEntityId.getBytes());
        // Find the RelyingPartyRegistration by the IdP's entity ID
        RelyingPartyRegistration registration = dynamicRelyingPartyRegistrationRepository.findUniqueByAssertingPartyEntityId(registrationId);
        return registration;
    }

    private String extractIssuerEntityId(HttpServletRequest request) {
        String issuerEntityId = SamlUtils.extractIssuerEntityId(request);
        return issuerEntityId; // Replace with actual extraction logic
    }
}
