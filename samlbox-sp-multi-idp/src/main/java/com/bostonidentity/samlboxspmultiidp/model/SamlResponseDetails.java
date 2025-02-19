package com.bostonidentity.samlboxspmultiidp.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public record SamlResponseDetails(
        String issuer,
        String subject,
        String responseId,
        String assertionId,
        Map<String, List<String>> attributes,
        Date notBefore,
        Date notOnOrAfter,
        boolean isSignatureValid
) {}
