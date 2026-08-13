package com.fan.mixmyfit.file;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.MapPropertySource;
import org.springframework.mock.web.MockMultipartFile;

class StoredFileServiceConfigurationTest {
    @TempDir
    Path uploadDir;

    @Test
    void uploadDirEnvironmentPropertyControlsStorageRoot() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test",
                    java.util.Map.of("UPLOAD_DIR", uploadDir.toString())));
            context.registerBean(PropertySourcesPlaceholderConfigurer.class, PropertySourcesPlaceholderConfigurer::new);
            context.register(StoredFileService.class);
            context.refresh();

            StoredFile stored = context.getBean(StoredFileService.class).store(new MockMultipartFile(
                    "file",
                    "shirt.png",
                    "image/png",
                    new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47}));

            assertThat(Path.of(stored.path())).startsWith(uploadDir);
            assertThat(Files.exists(Path.of(stored.path()))).isTrue();
        }
    }
}
