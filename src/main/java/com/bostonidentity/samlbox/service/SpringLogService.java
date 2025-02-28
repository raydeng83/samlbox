package com.bostonidentity.samlbox.service;

import org.apache.commons.io.input.ReversedLinesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class SpringLogService {
    private static final Logger logger = LoggerFactory.getLogger(SpringLogService.class);

    private final Path logDirectory;
    private final int maxAllowedLines = 10000; // Safety limit

    @Autowired
    public SpringLogService(@Value("${spring-log.file.directory:logs}") String logDir) {
        this.logDirectory = Paths.get(logDir).toAbsolutePath().normalize();
        validateLogDirectory();
    }

    private void validateLogDirectory() {
        if (!Files.isDirectory(logDirectory)) {
            throw new IllegalStateException("Invalid log directory: " + logDirectory);
        }
    }

    public LogFileContent getLastLines(String filename, int lines) throws IOException {
        validateRequest(filename, lines);
        Path filePath = resolveSafePath(filename);
        return readLastLines(filePath, Math.min(lines, maxAllowedLines));
    }

    private Path resolveSafePath(String filename) throws IOException {
        Path resolvedPath = logDirectory.resolve(filename).normalize();

        if (!resolvedPath.startsWith(logDirectory)) {
            throw new SecurityException("Attempted path traversal");
        }

        if (!Files.exists(resolvedPath)) {
            throw new FileNotFoundException("Log file not found");
        }

        return resolvedPath;
    }

    private void validateRequest(String filename, int lines) {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename cannot be empty");
        }
        if (lines <= 0) {
            throw new IllegalArgumentException("Line count must be positive");
        }
    }

    private LogFileContent readLastLines(Path filePath, int lines) throws IOException {
        try (ReversedLinesFileReader reader = new ReversedLinesFileReader(filePath.toFile(), StandardCharsets.UTF_8)) {
            List<String> content = new ArrayList<>(lines);
            String line;
            int count = 0;

            while (count < lines && (line = reader.readLine()) != null) {
                content.add(line);
                count++;
            }

            Collections.reverse(content);
            return new LogFileContent(
                    filePath.toString(),
                    content,
                    Files.size(filePath),
                    Files.getLastModifiedTime(filePath).toInstant()
            );
        } catch (Exception e) {
            logger.error("Error reading log file: {}", filePath, e);
            throw new IOException("Failed to read log file", e);
        }
    }

    public record LogFileContent(
            String filePath,
            List<String> lines,
            long fileSize,
            Instant lastModified
    ) {}
}
