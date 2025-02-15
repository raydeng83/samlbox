package com.bostonidentity.samlboxspmultiidp;

import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.security.x509.BasicX509Credential;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

public class SamlResponseProcessor {

    static {
        try {
            InitializationService.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenSAML", e);
        }
    }

    public static SamlResponseDetails processSamlResponse(String base64SamlResponse) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(sanitize(base64SamlResponse));
        String decodedXml = new String(decodedBytes, StandardCharsets.UTF_8);

        // Parse XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new java.io.ByteArrayInputStream(decodedXml.getBytes(StandardCharsets.UTF_8)));

        // Unmarshal XML into a SAML Response object
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        Response samlResponse = (Response) unmarshaller.unmarshall(element);

        if (samlResponse.getAssertions().isEmpty()) {
            throw new Exception("SAML Response does not contain any assertions");
        }

        // Extract details
        Assertion assertion = samlResponse.getAssertions().get(0);
        Map<String, List<String>> attributes = extractAttributes(assertion);
        String issuer = samlResponse.getIssuer().getValue();
        String subject = assertion.getSubject().getNameID().getValue();
        String responseId = samlResponse.getID();
        String assertionId = assertion.getID();
        Date notBefore = Date.from(assertion.getConditions().getNotBefore());
        Date notOnOrAfter = Date.from(assertion.getConditions().getNotOnOrAfter());
        boolean isSignatureValid = validateSignature(samlResponse);

        return new SamlResponseDetails(
                issuer, subject, responseId, assertionId, attributes, notBefore, notOnOrAfter, isSignatureValid
        );
    }

    private static Map<String, List<String>> extractAttributes(Assertion assertion) {
        Map<String, List<String>> attributesMap = new HashMap<>();

        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                List<String> values = attribute.getAttributeValues().stream()
                        .map(XMLObject::toString)
                        .toList();
                attributesMap.put(attribute.getName(), values);
            }
        }

        return attributesMap;
    }

    private static boolean validateSignature(Response samlResponse) {
        try {
            Assertion assertion = samlResponse.getAssertions().get(0);
            Signature signature = assertion.getSignature();
            if (signature == null) {
                return false;
            }

            X509Certificate cert = extractCertificate(assertion);
            if (cert == null) {
                return false;
            }

            BasicX509Credential credential = new BasicX509Credential(cert);
            SignatureValidator.validate(signature, credential);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static X509Certificate extractCertificate(Assertion assertion) throws Exception {
        Signature signature = assertion.getSignature();
        if (signature != null && signature.getKeyInfo() != null) {
            String base64Cert = signature.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0).getValue();
            byte[] decodedCert = Base64.getDecoder().decode(base64Cert.replaceAll("\\s+", ""));
            return (X509Certificate) CertificateFactory.getInstance("X.509")
                    .generateCertificate(new java.io.ByteArrayInputStream(decodedCert));
        }
        return null;
    }

    public static String sanitize(String input) {
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toCharArray()) {
            if (!Character.isISOControl(ch)) { // Removes all control characters
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}

