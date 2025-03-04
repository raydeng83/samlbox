package com.bostonidentity.samlbox.model;

import java.util.Date;
import java.util.List;
import java.util.Map;

public record SamlResponseDetails(
        String issuer,
        String subject,
        String responseId,
        String assertionId,
        Map<String, String> attributes,
        Date notBefore,
        Date notOnOrAfter,
        boolean isSignatureValid
) {
}
