//package com.bostonidentity.samlboxspmultiidp.config;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.security.saml2.core.Saml2X509Credential;
//import org.springframework.security.saml2.provider.service.authentication.logout.OpenSaml4LogoutResponseValidator;
//import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutRequest;
//import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutResponse;
//import org.springframework.security.saml2.provider.service.authentication.logout.Saml2LogoutResponseValidator;
//import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
//import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
//import org.springframework.security.web.authentication.logout.LogoutHandler;
//import org.springframework.stereotype.Component;
//
//@Component
//public class Saml2LogoutHandler implements LogoutHandler {
//    private static final Logger logger = LoggerFactory.getLogger(Saml2LogoutHandler.class);
//    private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;
//    private final Saml2X509Credential signingCredential;
//
//    @Autowired
//    public Saml2LogoutHandler(
//            RelyingPartyRegistrationRepository relyingPartyRegistrationRepository,
//            @Qualifier("signingCredential") Saml2X509Credential signingCredential
//    ) {
//        this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
//        this.signingCredential = signingCredential;
//    }
//
//    // Required LogoutHandler method
//    @Override
//    public void logout(HttpServletRequest request, HttpServletResponse response,
//                       Authentication authentication) {
//        // Add your session cleanup logic here
//        SecurityContextHolder.clearContext();
//        logger.info("User logged out successfully");
//    }
//
//    // SAML-specific methods
//    public String generateLogoutRequest(Authentication authentication) {
//        RelyingPartyRegistration registration = relyingPartyRegistrationRepository.findByRegistrationId("your-registration-id");
//
//        Saml2LogoutRequest logoutRequest = Saml2LogoutRequest.withRelyingPartyRegistration(registration)
//                .build();
//
//        return logoutRequest.getSamlRequest();
//    }
//
////    public void handleLogoutResponse(HttpServletRequest request) {
////        Saml2LogoutResponseValidator validator = new OpenSaml4LogoutResponseValidator();
////        Saml2LogoutResponse response = validator.validate(request);
////        logger.info("Received logout response from {}", response.());
////    }
//}
