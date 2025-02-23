package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.model.IdpConfig;
import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlbox.repository.IdpConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdpConfigService {

    @Autowired
    private DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    @Autowired
    private IdpConfigRepository idpConfigRepository;

    public void updateIdpConfig(IdpConfig idpConfig) {
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
