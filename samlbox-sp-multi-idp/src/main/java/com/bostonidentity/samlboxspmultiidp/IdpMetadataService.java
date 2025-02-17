package com.bostonidentity.samlboxspmultiidp;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IdpMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(IdpMetadataService.class);

    private final Path storageDir = Paths.get("./idp-metadata");
    private final IdpMetadataRepository idpMetadataRepository;
    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    public IdpMetadataService(IdpMetadataRepository idpMetadataRepository, DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository) throws IOException {
        this.idpMetadataRepository = idpMetadataRepository;
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        this.dynamicRelyingPartyRegistrationRepository = dynamicRelyingPartyRegistrationRepository;
    }

    public String saveMetadata(MultipartFile file) throws IOException {
        try {
            // Parse the entity ID from the metadata
            String entityId = parseEntityId(file.getInputStream());

            // Generate a unique registrationId
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

            // Save the metadata file
            String filename = registrationId + ".xml";
            Path target = storageDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            // Save the metadata to the database
            IdpMetadata metadata = new IdpMetadata();
            metadata.setEntityId(entityId);
            metadata.setRegistrationId(registrationId);
            metadata.setMetadataFilePath(target.toString());
            idpMetadataRepository.save(metadata);

            dynamicRelyingPartyRegistrationRepository.addRegistration(metadata);

            return registrationId;
        } catch (Exception e) {
            throw new IOException("Invalid metadata", e);
        }
    }

    @Transactional
    public void deleteMetadata(String id) throws IOException {
        String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(id.getBytes());
        Path target = storageDir.resolve(registrationId + ".xml");
        Files.deleteIfExists(target);

        // Find the metadata entry in the database
        Optional<IdpMetadata> metadata = idpMetadataRepository.findByRegistrationId(registrationId);
        if (metadata.isPresent()) {
            // Delete the metadata file from disk
            Path metadataFilePath = Paths.get(metadata.get().getMetadataFilePath());
            Files.deleteIfExists(metadataFilePath);

            logger.info("Deleted metadata file: {}", metadataFilePath);

            // Delete the metadata entry from the database
            idpMetadataRepository.delete(metadata.get());
        } else {
            logger.warn("No metadata found for registrationId: {}", registrationId);
            throw new IllegalArgumentException("No metadata found for registrationId: " + registrationId);
        }
    }

    public List<Path> loadMetadataFiles() {
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    public String getRegistrationId(String entityId) {
        Optional<IdpMetadata> metadata = idpMetadataRepository.findByEntityId(entityId);
        return metadata.map(IdpMetadata::getRegistrationId).orElse(null);
    }



    private String parseEntityId(InputStream inputStream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(inputStream);
        return doc.getDocumentElement().getAttribute("entityID");
    }

    // IdpMetadataService.java
//    public IdpMetadataDetails parseMetadataDetails(String registrationId) throws Exception {
//        IdpMetadataDetails details = new IdpMetadataDetails();
//        Optional<IdpMetadata> metadata = idpMetadataRepository.findByRegistrationId(registrationId);
//
//        if (metadata.isPresent()) {
//            Path path = Paths.get(metadata.get().getMetadataFilePath());
//            Document doc = DocumentBuilderFactory.newInstance()
//                    .newDocumentBuilder()
//                    .parse(Files.newInputStream(path));
//
//            // Parse XML elements
//            Element entityDescriptor = doc.getDocumentElement();
//            details.setEntityId(entityDescriptor.getAttribute("entityID"));
//
//            NodeList idpSsoDescriptors = entityDescriptor.getElementsByTagNameNS(
//                    "urn:oasis:names:tc:SAML:2.0:metadata", "IDPSSODescriptor");
//
//            if (idpSsoDescriptors.getLength() > 0) {
//                Element idpSso = (Element) idpSsoDescriptors.item(0);
//                Element ssoService = (Element) idpSso.getElementsByTagNameNS(
//                        "urn:oasis:names:tc:SAML:2.0:metadata", "SingleSignOnService").item(0);
//
//                details.setSsoUrl(ssoService.getAttribute("Location"));
//                details.setProtocolBinding(ssoService.getAttribute("Binding"));
//            }
//
//            // Parse certificates
//            NodeList keyDescriptors = entityDescriptor.getElementsByTagNameNS(
//                    "urn:oasis:names:tc:SAML:2.0:metadata", "KeyDescriptor");
//            for (int i = 0; i < keyDescriptors.getLength(); i++) {
//                Element keyDescriptor = (Element) keyDescriptors.item(i);
//                Element x509Certificate = (Element) keyDescriptor.getElementsByTagNameNS(
//                        "http://www.w3.org/2000/09/xmldsig#", "X509Certificate").item(0);
//                details.getCertificates().add(x509Certificate.getTextContent());
//            }
//        }
//
//        return details;
//    }
}
