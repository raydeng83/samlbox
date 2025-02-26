package com.bostonidentity.samlbox.service;

import com.bostonidentity.samlbox.model.IdpMetadata;
import com.bostonidentity.samlbox.model.SpMetadata;
import com.bostonidentity.samlbox.repository.SpMetadataRepository;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
public class SpMetadataService {

    private final Path storageDir = Paths.get("./sp-metadata");
    private final SpMetadataRepository spMetadataRepository;

    public SpMetadataService(SpMetadataRepository spMetadataRepository) throws IOException {
        this.spMetadataRepository = spMetadataRepository;

        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
    }

    @Transactional
    public String saveMetadata(MultipartFile file) throws IOException {
        try {
            String entityId = SamlUtils.parseEntityId(file.getInputStream());
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

            // Check if SP exists
            SpMetadata existingIdp = spMetadataRepository.findByEntityId(entityId).orElse(null);

            if (existingIdp != null) {
                deleteSpMetadata(entityId);
            }

            String filename = registrationId + ".xml";
            Path target = storageDir.resolve(filename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            SpMetadata metadata = new SpMetadata();
            metadata.setEntityId(entityId);
            metadata.setRegistrationId(registrationId);
            metadata.setMetadataFilePath(target.toString());
            spMetadataRepository.save(metadata);

            return entityId;
        } catch (Exception e) {
            throw new IOException("Invalid metadata", e);
        }
    }

    @Transactional
    public void deleteSpMetadata(String entityId) throws IOException {
        String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());
        Path target = storageDir.resolve(registrationId + ".xml");
        Files.deleteIfExists(target);

        Optional<SpMetadata> metadata = spMetadataRepository.findByRegistrationId(registrationId);
        if (metadata.isPresent()) {
            Path metadataFilePath = Paths.get(metadata.get().getMetadataFilePath());
            Files.deleteIfExists(metadataFilePath);

            spMetadataRepository.delete(metadata.get());
        } else {
            throw new IllegalArgumentException("No metadata found for registrationId: " + registrationId);
        }
    }

    public String getRawXmlContent(String registrationId) throws Exception {
        Optional<SpMetadata> metadata = spMetadataRepository.findByRegistrationId(registrationId);
        if (metadata.isPresent()) {
            Path path = Paths.get(metadata.get().getMetadataFilePath());
            return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
        }
        throw new FileNotFoundException("SP Metadata file not found");
    }

    public String getFormattedXml(String registrationId) throws Exception {
        String rawXml = getRawXmlContent(registrationId);
        return SamlUtils.formatXml(rawXml);
    }

    public ResponseEntity<Resource> downloadXml(String registrationId) throws Exception {
        Optional<SpMetadata> metadata = spMetadataRepository.findByRegistrationId(registrationId);
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
