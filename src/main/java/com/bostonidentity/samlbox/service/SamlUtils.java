package com.bostonidentity.samlbox.service;

import jakarta.servlet.http.HttpServletRequest;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class SamlUtils {

    static {
        try {
            // Initialize OpenSAML
            InitializationService.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize OpenSAML", e);
        }
    }

    // Method to extract issuer entity ID from SAML response
        public static String extractIssuerEntityId(HttpServletRequest request) {
            try {
                // Get SAMLResponse parameter from request
                String samlResponse = request.getParameter("SAMLResponse");
                if (samlResponse == null) {
                    throw new IllegalArgumentException("SAMLResponse parameter not found");
                }

                // Determine binding type from HTTP method
                String httpMethod = request.getMethod().toUpperCase();
                byte[] decodedBytes;

                if ("GET".equals(httpMethod)) {
                    decodedBytes = Base64.getDecoder().decode(samlResponse);
                    decodedBytes = inflate(decodedBytes);
                } else if ("POST".equals(httpMethod)) {
                    decodedBytes = Base64.getMimeDecoder().decode(sanitize(samlResponse));
                } else {
                    throw new IllegalArgumentException("Unsupported HTTP method: " + httpMethod);
                }

                // Parse the SAML response XML
                Response response = parseSamlResponse(decodedBytes);

                // Extract the issuer entity ID
                Issuer issuer = response.getIssuer();
                return issuer != null ? issuer.getValue() : null;

            } catch (Exception e) {
                throw new RuntimeException("Failed to extract issuer entity ID", e);
            }
        }

    private static byte[] inflate(byte[] input) throws DataFormatException {
        Inflater inflater = new Inflater(true); // Nowrap true for zlib header
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];

        try {
            inflater.setInput(input);
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                outputStream.write(buffer, 0, count);
            }
        } finally {
            inflater.end();
        }
        return outputStream.toByteArray();
    }

    private static String getIssuer(byte[] xmlBytes) throws Exception {
        // Your existing parsing logic
        Response response = parseSamlResponse(xmlBytes);
        Issuer issuer = response.getIssuer();
        return issuer != null ? issuer.getValue() : null;
    }

    public static String extractRelayState(HttpServletRequest request) {
        return request.getParameter("RelayState"); // Case-sensitive parameter name
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

    private static Response parseSamlResponse(byte[] samlResponseBytes) throws Exception {
        // Parse the XML into a DOM element
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Element element = factory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(samlResponseBytes))
                .getDocumentElement();

        // Unmarshall the XML into an OpenSAML Response object
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        return (Response) unmarshaller.unmarshall(element);
    }
}
