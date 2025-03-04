package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.model.IdpConfig;
import com.bostonidentity.samlbox.model.IdpMetadata;
import com.bostonidentity.samlbox.repository.DynamicRelyingPartyRegistrationRepository;
import com.bostonidentity.samlbox.repository.IdpConfigRepository;
import com.bostonidentity.samlbox.repository.IdpMetadataRepository;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Optional;

@Service
public class IdpMetadataService {

    private static final Logger logger = LoggerFactory.getLogger(IdpMetadataService.class);

    private final IdpConfigRepository idpConfigRepository;
    private final Path storageDir = Paths.get("./idp-metadata");
    private final IdpMetadataRepository idpMetadataRepository;
    private final DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository;

    public IdpMetadataService(IdpMetadataRepository idpMetadataRepository, DynamicRelyingPartyRegistrationRepository dynamicRelyingPartyRegistrationRepository, IdpConfigRepository idpConfigRepository) throws IOException {
        this.idpMetadataRepository = idpMetadataRepository;
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
        this.dynamicRelyingPartyRegistrationRepository = dynamicRelyingPartyRegistrationRepository;
        this.idpConfigRepository = idpConfigRepository;
    }

    @Transactional
    public String saveMetadata(MultipartFile file) throws IOException {
        try {
            // Parse the entity ID from the metadata
            String entityId = SamlUtils.parseEntityId(file.getInputStream());
            String registrationId;

            // Check if IDP exists
            IdpMetadata existingIdp = idpMetadataRepository.findByEntityId(entityId).orElse(null);

            if (existingIdp != null) {
                deleteMetadata(entityId); // removing existing one before creating a new one
            }
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
            idpConfig.setSsoLocationUrl(registration.getAssertingPartyMetadata().getSingleSignOnServiceLocation());
            idpConfig.setSsoBinding(registration.getAssertingPartyMetadata().getSingleSignOnServiceBinding().toString());
            idpConfig.setSloLocationUrl(registration.getAssertingPartyMetadata().getSingleLogoutServiceLocation());
            idpConfig.setSsoBinding(registration.getAssertingPartyMetadata().getSingleLogoutServiceBinding().toString());
            idpConfigRepository.save(idpConfig);

            dynamicRelyingPartyRegistrationRepository.reloadRegistrations();

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
        return SamlUtils.formatXml(rawXml);
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
