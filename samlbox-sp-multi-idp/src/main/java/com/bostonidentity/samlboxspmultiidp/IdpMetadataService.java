package com.bostonidentity.samlboxspmultiidp;

import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private IdpConfigRepository idpConfigRepository;

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
            String registrationId;

            // Check if IDP exists
            IdpMetadata existingIdp = idpMetadataRepository.findByEntityId(entityId).orElse(null);

            if (existingIdp != null) {
                 registrationId = existingIdp.getRegistrationId();

                // Save the metadata file
                String filename = registrationId + ".xml";
                Path target = storageDir.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                existingIdp.setEntityId(entityId);
                existingIdp.setRegistrationId(registrationId);
                existingIdp.setMetadataFilePath(target.toString());
                idpMetadataRepository.save(existingIdp);

                RelyingPartyRegistration registration = dynamicRelyingPartyRegistrationRepository.updateRegistration(existingIdp);

                IdpConfig idpConfig = idpConfigRepository.findByEntityId(entityId).get();
                idpConfig.setEntityId(entityId);
                idpConfig.setRegistrationId(registrationId);
                idpConfig.setSsoLocationUrl(registration.getAssertingPartyDetails().getSingleSignOnServiceLocation());
                idpConfigRepository.save(idpConfig);

            } else {
                // Generate a unique registrationId
                 registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

                // Save the metadata file
                String filename = registrationId + ".xml";
                Path target = storageDir.resolve(filename);
                Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

                IdpMetadata metadata = new IdpMetadata();
                metadata.setEntityId(entityId);
                metadata.setRegistrationId(registrationId);
                metadata.setMetadataFilePath(target.toString());
                idpMetadataRepository.save(metadata);

                RelyingPartyRegistration registration = dynamicRelyingPartyRegistrationRepository.addRegistration(metadata);

                IdpConfig idpConfig = new IdpConfig();
                idpConfig.setEntityId(entityId);
                idpConfig.setRegistrationId(registrationId);
                idpConfig.setSsoLocationUrl(registration.getAssertingPartyDetails().getSingleSignOnServiceLocation());
                idpConfigRepository.save(idpConfig);
            }

            return entityId;
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

            if (idpConfigRepository.findByEntityId(id).isPresent()) {
                idpConfigRepository.delete(idpConfigRepository.findByEntityId(id).get());
            }
            idpMetadataRepository.delete(metadata.get());
        } else {
            logger.warn("No metadata found for registrationId: {}", registrationId);
            throw new IllegalArgumentException("No metadata found for registrationId: " + registrationId);
        }
    }

    private String parseEntityId(InputStream inputStream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(inputStream);
        return doc.getDocumentElement().getAttribute("entityID");
    }

    public String getRawXmlContent(String registrationId) throws Exception {
        Optional<IdpMetadata> metadata = idpMetadataRepository.findByRegistrationId(registrationId);
        if (metadata.isPresent()) {
            Path path = Paths.get(metadata.get().getMetadataFilePath());
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException("Metadata file not found");
    }

    public String getFormattedXml(String registrationId) throws Exception {
        String rawXml = getRawXmlContent(registrationId);
        return formatXml(rawXml);
    }

    private String formatXml(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false); // Prevent XXE
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(xml)));

        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute("indent-number", 2); // Set indentation to 2 spaces
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString().trim();
    }

    public ResponseEntity<Resource> downloadXml(String registrationId) throws Exception {
        Optional<IdpMetadata> metadata = idpMetadataRepository.findByRegistrationId(registrationId);
        if (metadata.isPresent()) {
            Path path = Paths.get(metadata.get().getMetadataFilePath());
            Resource resource = new UrlResource(path.toUri());

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_XML)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);
        }
        throw new FileNotFoundException("Metadata file not found");
    }
}
