package com.bostonidentity.samlboxspmultiidp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class IdpConfigService {

    @Autowired
    private DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;


}
