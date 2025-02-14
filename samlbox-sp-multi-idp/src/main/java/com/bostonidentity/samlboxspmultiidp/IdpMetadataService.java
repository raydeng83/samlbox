package com.bostonidentity.samlboxspmultiidp;

import org.springframework.stereotype.Service;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class IdpMetadataService {
    private final Path storageDir = Paths.get("./idp-metadata");

    public IdpMetadataService() throws IOException {
        if (!Files.exists(storageDir)) {
            Files.createDirectories(storageDir);
        }
    }

    public void saveMetadata(String filename, InputStream content) throws IOException {
        try {
            // Read the entire InputStream into a byte array
            byte[] contentBytes = content.readAllBytes();

            // Use a new ByteArrayInputStream to parse XML (to avoid consuming the original stream)
            String entityId = parseEntityId(new ByteArrayInputStream(contentBytes));

            // Encode entityId to a filename-safe format
            filename = Base64.getUrlEncoder().withoutPadding().encodeToString(entityId.getBytes()) + ".xml";

            // Use another ByteArrayInputStream to copy the original content to storage
            Files.copy(new ByteArrayInputStream(contentBytes), storageDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
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

    private String parseEntityId(InputStream inputStream) throws Exception {
        Document doc = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(inputStream);
        return doc.getDocumentElement().getAttribute("entityID");
    }
}
