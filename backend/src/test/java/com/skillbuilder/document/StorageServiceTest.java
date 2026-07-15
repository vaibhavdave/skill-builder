package com.skillbuilder.document;

import com.skillbuilder.config.StorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceTest {

    @TempDir
    Path tempDir;

    private StorageService storage;

    @BeforeEach
    void setUp() {
        storage = new StorageService(new StorageProperties(tempDir.toString()));
    }

    @Test
    void storesUnderSkillDirectoryAndLoadsBack() throws Exception {
        var file = new MockMultipartFile("file", "my book.pdf", "application/pdf", "content".getBytes());

        String relative = storage.store(7L, file);

        assertThat(relative).startsWith("7/").endsWith("-my book.pdf");
        assertThat(storage.load(relative).getContentAsByteArray()).isEqualTo("content".getBytes());
    }

    @Test
    void sanitizesHostileFilenames() {
        var file = new MockMultipartFile("file", "../../etc/passwd", "text/plain", "x".getBytes());

        String relative = storage.store(1L, file);

        // Only the final path segment is kept and it stays inside the root
        assertThat(relative).startsWith("1/").doesNotContain("..");
        assertThat(Files.exists(tempDir.resolve(relative))).isTrue();
    }

    @Test
    void rejectsPathTraversalOnLoad() {
        assertThatThrownBy(() -> storage.load("../outside.txt"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteRemovesFileAndSkillDirectoryIsRemovable() {
        var file = new MockMultipartFile("file", "a.txt", "text/plain", "x".getBytes());
        String relative = storage.store(3L, file);

        storage.delete(relative);
        storage.deleteSkillDirectory(3L);

        assertThat(Files.exists(tempDir.resolve(relative))).isFalse();
        assertThat(Files.exists(tempDir.resolve("3"))).isFalse();
    }
}
