package com.bostonidentity.samlboxspmultiidp;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
        Path target = storageDir.resolve(filename);
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
    }

    public List<Path> loadMetadataFiles() {
        try (Stream<Path> paths = Files.list(storageDir)) {
            return paths.filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }
}
