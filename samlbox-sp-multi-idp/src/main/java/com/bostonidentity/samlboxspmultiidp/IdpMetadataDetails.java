package com.bostonidentity.samlboxspmultiidp;


import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class IdpMetadataDetails {
    private String entityId;
    private String ssoUrl;
    private List<String> certificates = new ArrayList<>();
    private String protocolBinding;
    private LocalDateTime validUntil;
}
