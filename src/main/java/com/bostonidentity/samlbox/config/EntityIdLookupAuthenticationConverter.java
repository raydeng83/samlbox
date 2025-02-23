package com.bostonidentity.samlbox.config;

import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.saml2.core.Saml2Error;
import org.springframework.security.saml2.core.Saml2ErrorCodes;
import org.springframework.security.saml2.provider.service.authentication.AbstractSaml2AuthenticationRequest;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationRequestRepository;
import org.springframework.security.saml2.provider.service.web.Saml2AuthenticationTokenConverter;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class EntityIdLookupAuthenticationConverter implements AuthenticationConverter {

    @Autowired
    private EntityIdRelyingPartyRegistrationResolver relyingPartyRegistrationResolver;

    @Autowired
    private Saml2AuthenticationRequestRepository<AbstractSaml2AuthenticationRequest> authnRequestRepository;


//    @Autowired
//    private Saml2AuthenticationTokenConverter saml2AuthenticationTokenConverter;

    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    public EntityIdLookupAuthenticationConverter(
            DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository
    ) {
        this.dynamicRelyingPartyRegistrationRepository = dynamicRelyingPartyRegistrationRepository;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        Saml2AuthenticationToken token =
                new Saml2AuthenticationTokenConverter(relyingPartyRegistrationResolver).convert(request);
        if (token == null) {
            return null; // Not a SAML response
        }

        // Retrieve the original AuthnRequest from the repository
        AbstractSaml2AuthenticationRequest authnRequest =
                authnRequestRepository.loadAuthenticationRequest(request);

        // Extract the IdP's entity ID (issuer) from the SAML response
        String samlXml = token.getSaml2Response();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = null;
        try {
            builder = factory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        Document document = null;
        try {
            document = builder.parse(new java.io.ByteArrayInputStream(samlXml.getBytes(StandardCharsets.UTF_8)));
        } catch (SAXException | IOException e) {
            throw new RuntimeException(e);
        }

        // Unmarshal XML into a SAML Response object
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        Response samlResponse = null;
        try {
            samlResponse = (Response) unmarshaller.unmarshall(element);
        } catch (UnmarshallingException e) {
            throw new RuntimeException(e);
        }

//        if (samlResponse.getAssertions().isEmpty()) {
//            try {
//                throw new Exception("SAML Response does not contain any assertions");
//            } catch (Exception e) {
//                throw new RuntimeException(e);
//            }
//        }
        String issuerId = samlResponse.getIssuer().getValue();
        String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(issuerId.getBytes());

        // Find the RelyingPartyRegistration by the IdP's entity ID
        RelyingPartyRegistration registration = dynamicRelyingPartyRegistrationRepository.findUniqueByAssertingPartyEntityId(registrationId);
        if (registration == null) {
            Saml2Error error = new Saml2Error(
                    Saml2ErrorCodes.RELYING_PARTY_REGISTRATION_NOT_FOUND,
                    "No registration found for IdP entity ID: " + issuerId
            );
            throw new Saml2AuthenticationException(error);
        }

        // Create a new authentication token with the resolved registration
        return new Saml2AuthenticationToken(
                registration,
                token.getSaml2Response(),
                authnRequest
        );
    }
}