//package com.bostonidentity.samlboxspmultiidp.controller;
//
//import com.bostonidentity.samlboxspmultiidp.config.Saml2LogoutHandler;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.ResponseEntity;
//import org.springframework.security.core.Authentication;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.io.IOException;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//
//@RestController
//public class LogoutController {
//    private final Saml2LogoutHandler logoutHandler;
//
//    @Autowired
//    public LogoutController(Saml2LogoutHandler logoutHandler) {
//        this.logoutHandler = logoutHandler;
//    }
//
//    @GetMapping("/logout")
//    public void initiateLogout(HttpServletResponse response, Authentication authentication)
//            throws IOException {
//        String samlRequest = logoutHandler.generateLogoutRequest(authentication);
//
//        // Redirect to IdP's logout endpoint
//        String redirectUrl = "https://idp/logout?SAMLRequest=" + URLEncoder.encode(samlRequest, StandardCharsets.UTF_8);
//        response.sendRedirect(redirectUrl);
//    }
//
//    @PostMapping("/logout/saml2/slo")
//    public ResponseEntity<String> handleLogoutResponse(HttpServletRequest request) {
////        logoutHandler.handleLogoutResponse(request);
//        return ResponseEntity.ok("Logged out successfully");
//    }
//}
