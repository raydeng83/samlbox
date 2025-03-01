package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.model.SamlResponseDetails;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.ParserPool;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.saml2.core.*;
import org.opensaml.saml.saml2.encryption.Decrypter;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.encryption.support.ChainingEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.DecryptionException;
import org.opensaml.xmlsec.encryption.support.InlineEncryptedKeyResolver;
import org.opensaml.xmlsec.encryption.support.SimpleRetrievalMethodEncryptedKeyResolver;
import org.opensaml.xmlsec.keyinfo.KeyInfoCredentialResolver;
import org.opensaml.xmlsec.keyinfo.impl.StaticKeyInfoCredentialResolver;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;

import static com.bostonidentity.samlbox.service.SamlUtils.sanitize;

@Component
public class SamlResponseProcessor {
    private static final Logger logger = LoggerFactory.getLogger(SamlResponseProcessor.class);

    private static Saml2X509Credential decryptingCredential;

    static {
        try {
            InitializationService.initialize();

            // Then add BouncyCastle provider
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenSAML", e);
        }
    }

    public static ParserPool getParserPool() {
        BasicParserPool pool = new BasicParserPool();
        pool.setMaxPoolSize(50);
        return pool;
    }

    public SamlResponseProcessor(@Qualifier("decryptingCredential") Saml2X509Credential decryptingCredential) {
        this.decryptingCredential = decryptingCredential;
    }

    public static SamlResponseDetails processSamlResponse(String base64SamlResponse) throws Exception {
        byte[] decodedBytes = Base64.getDecoder().decode(sanitize(base64SamlResponse));
        String decodedXml = new String(decodedBytes, StandardCharsets.UTF_8);

        // Parse XML
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new ByteArrayInputStream(decodedXml.getBytes(StandardCharsets.UTF_8)));

        // Unmarshal XML into a SAML Response object
        Element element = document.getDocumentElement();
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        Response samlResponse = (Response) unmarshaller.unmarshall(element);

        // DECRYPTION STEP - New code
        if (!samlResponse.getEncryptedAssertions().isEmpty()) {
            logger.info("Found encrypted assertions, attempting decryption");
            samlResponse = decryptAssertions(samlResponse);
        }

        if (samlResponse.getAssertions().isEmpty()) {
            throw new Exception("SAML Response does not contain any assertions");
        }

        // Extract details
        Assertion assertion = samlResponse.getAssertions().get(0);
        Map<String, String> attributes = extractAttributes(assertion);
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

    private static Map<String, String> extractAttributes(Assertion assertion) {
        Map<String, String> attributesMap = new HashMap<>();

        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                List<String> values = attribute.getAttributeValues().stream()
                        .map(XMLObject::toString)
                        .toList();
                XSString xsString = (XSString) attribute.getAttributeValues().getFirst();
                String value = xsString.getValue();
                attributesMap.put(attribute.getName(), value);
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
                    .generateCertificate(new ByteArrayInputStream(decodedCert));
        }
        return null;
    }

    private static Element extractEncryptedAssertionElement(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml.getBytes())).getDocumentElement();
    }

    private static Response decryptAssertions(Response encryptedResponse) throws Exception {
        BasicX509Credential credential = new BasicX509Credential(
                decryptingCredential.getCertificate(),
                decryptingCredential.getPrivateKey()
        );

        Decrypter decrypter = createDecrypter(credential);
        decrypter.setJCAProviderName("BC");
        decrypter.setRootInNewDocument(true);
        List<Assertion> decryptedAssertions = new ArrayList<>();

        for (EncryptedAssertion encryptedAssertion : encryptedResponse.getEncryptedAssertions()) {
            try {
                Assertion assertion = decrypter.decrypt(encryptedAssertion);
                decryptedAssertions.add(assertion);
                logger.info("Successfully decrypted assertion");
            } catch (DecryptionException e) {
                handleDecryptionError(encryptedAssertion, e);
            }
        }

        return buildDecryptedResponse(encryptedResponse, decryptedAssertions);
    }

    private static Decrypter createDecrypter(BasicX509Credential credential) {
        KeyInfoCredentialResolver keyInfoResolver = new StaticKeyInfoCredentialResolver(credential);

        return new Decrypter(
                new StaticKeyInfoCredentialResolver(credential),
                keyInfoResolver,
                new ChainingEncryptedKeyResolver(Arrays.asList(
                        new InlineEncryptedKeyResolver(),
                        new SimpleRetrievalMethodEncryptedKeyResolver()
                )),
                null,
                null
        );
    }

    private static void handleDecryptionError(EncryptedAssertion encryptedAssertion, DecryptionException e) throws Exception {
        logger.error("Decryption error details:", e);
        throw new Exception("Failed to decrypt assertion", e);
    }

    private static Response buildDecryptedResponse(Response original, List<Assertion> decryptedAssertions)
            throws MarshallingException, UnmarshallingException {
        Response decryptedResponse = (Response) XMLObjectSupport.cloneXMLObject(original);
        decryptedResponse.getEncryptedAssertions().clear();
        decryptedResponse.getAssertions().addAll(decryptedAssertions);
        return decryptedResponse;
    }

    private static BasicX509Credential getDecryptionCredential() throws Exception {
        if (decryptingCredential == null) {
            throw new IllegalStateException("Decryption credential not initialized");
        }

        return new BasicX509Credential(
                decryptingCredential.getCertificate(),
                decryptingCredential.getPrivateKey()
        );
    }

    private static Response cloneResponse(Response original) throws MarshallingException, UnmarshallingException {
        // Create a new response object with same metadata
        Response newResponse = (Response) XMLObjectSupport.cloneXMLObject(original);
        newResponse.getEncryptedAssertions().clear();
        return newResponse;
    }


}

