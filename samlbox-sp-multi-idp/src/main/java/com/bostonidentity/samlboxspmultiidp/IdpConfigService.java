package com.bostonidentity.samlboxspmultiidp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdpConfigService {

    @Autowired
    private DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    @Autowired
    private IdpConfigRepository idpConfigRepository;

    public void updateIdpConfig (IdpConfig idpConfig) {
        IdpConfig existingIdpConfig = idpConfigRepository.findByEntityId(idpConfig.getEntityId())
                .orElseThrow(() -> new IllegalArgumentException("IDP not found"));

        existingIdpConfig.setSsoBinding(idpConfig.getSsoBinding());
        existingIdpConfig.setSsoLocationUrl(idpConfig.getSsoLocationUrl());
        existingIdpConfig.setSignRequests(idpConfig.isSignRequests());
        existingIdpConfig.setNameIdFormat(idpConfig.getNameIdFormat());

        IdpConfig savedIdpConfig = idpConfigRepository.save(existingIdpConfig);

        dynamicRelyingPartyRegistrationRepository.updateRegistration(savedIdpConfig);
    }
}
