package com.bostonidentity.samlboxspmultiidp;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;

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
    private final Path storageDir = Paths.get("./idp-metadata");
    private final IdpMetadataRepository idpMetadataRepository;

    public IdpMetadataService(IdpMetadataRepository idpMetadataRepository) throws IOException {
        this.idpMetadataRepository = idpMetadataRepository;
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
    }

    @Transactional
    public void saveMetadata(MultipartFile file) throws IOException {
        try {
            // Parse the entity ID from the metadata
            String entityId = parseEntityId(file.getInputStream());

            // Generate a unique registrationId
            String registrationId = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes());

            // Use a new ByteArrayInputStream to parse XML (to avoid consuming the original stream)
//            String entityId = parseEntityId(new ByteArrayInputStream(contentBytes));
            // Encode entityId to a filename-safe format
//            filename = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes()) + ".xml";
            // Use another ByteArrayInputStream to copy the original content to storage
//            Files.copy(new ByteArrayInputStream(contentBytes), storageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);


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
        } catch (Exception e) {
            throw new IOException("Invalid metadata", e);
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

    public void deleteMetadata(String registrationId) throws IOException {
        Path target = storageDir.resolve(Base64.getUrlEncoder().withoutPadding().encodeToString(registrationId.getBytes()) + ".xml");
        Files.deleteIfExists(target);
    }

    public String getRegistrationId(String entityId) {
        Optional<IdpMetadata> metadata = idpMetadataRepository.findByEntityId(entityId);
        return metadata.map(IdpMetadata::getRegistrationId).orElse(null);
    }

    public List<IdpMetadata> getAllMetadata() {
        return idpMetadataRepository.findAll();
    }

    private String parseEntityId(InputStream inputStream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(inputStream);
        return doc.getDocumentElement().getAttribute("entityID");
    }
}
