package com.skillbuilder.document;

import com.skillbuilder.config.StorageProperties;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Stores uploaded files under {root}/{skillId}/{uuid}-{sanitized-filename}.
 * Postgres keeps only the path relative to the root.
 */
@Service
public class StorageService {

    private final Path root;

    public StorageService(StorageProperties properties) {
        this.root = Path.of(properties.root()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create storage root " + root, e);
        }
    }

    public String store(Long skillId, MultipartFile file) {
        String original = StringUtils.hasText(file.getOriginalFilename())
                ? Path.of(file.getOriginalFilename()).getFileName().toString()
                : "file";
        String safeName = original.replaceAll("[^\\w.\\-() ]", "_");
        String relative = skillId + "/" + UUID.randomUUID() + "-" + safeName;
        Path target = resolve(relative);
        try {
            Files.createDirectories(target.getParent());
            try (var in = file.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file " + safeName, e);
        }
        return relative;
    }

    public Resource load(String relativePath) {
        Path path = resolve(relativePath);
        if (!Files.exists(path)) {
            throw new IllegalArgumentException("Stored file is missing on disk");
        }
        return new PathResource(path);
    }

    public void delete(String relativePath) {
        try {
            Files.deleteIfExists(resolve(relativePath));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete stored file", e);
        }
    }

    public void deleteSkillDirectory(Long skillId) {
        try {
            FileSystemUtils.deleteRecursively(root.resolve(String.valueOf(skillId)));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to delete skill directory", e);
        }
    }

    private Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Invalid storage path");
        }
        return path;
    }
}
