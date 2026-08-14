package com.fan.mixmyfit.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DockerDistributionTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void dockerComposeFileDefinesFullLocalStackAndPersistentVolumes() throws IOException {
        Path composeFile = projectRoot.resolve("docker-compose.yml");

        assertThat(composeFile).exists();
        String compose = Files.readString(composeFile);

        assertThat(compose)
                .contains("mysql:")
                .contains("backend:")
                .contains("frontend:")
                .contains("mysql-data:")
                .contains("upload-data:")
                .contains("SPRING_DATASOURCE_URL")
                .contains("UPLOAD_DIR");
    }

    @Test
    void backendDockerfileFailsFastWhenDatabaseConfigurationIsMissing() throws IOException {
        Path dockerfile = projectRoot.resolve("backend").resolve("Dockerfile");

        assertThat(dockerfile).exists();
        String backendDockerfile = Files.readString(dockerfile);

        assertThat(backendDockerfile)
                .contains("SPRING_DATASOURCE_URL")
                .contains("SPRING_DATASOURCE_USERNAME")
                .contains("SPRING_DATASOURCE_PASSWORD")
                .contains("Missing required environment variable");
    }

    @Test
    void frontendAndBackendDockerfilesExposeExpectedRuntimePorts() throws IOException {
        Path backendDockerfile = projectRoot.resolve("backend").resolve("Dockerfile");
        Path frontendDockerfile = projectRoot.resolve("frontend").resolve("Dockerfile");

        assertThat(backendDockerfile).exists();
        assertThat(frontendDockerfile).exists();
        assertThat(Files.readString(backendDockerfile)).contains("EXPOSE 8080");
        assertThat(Files.readString(frontendDockerfile)).contains("EXPOSE 80");
    }

    @Test
    void backendHealthcheckUsesJavaRuntimeInsteadOfUndeclaredHttpUtilities() throws IOException {
        String compose = Files.readString(projectRoot.resolve("docker-compose.yml"));
        String backendDockerfile = Files.readString(projectRoot.resolve("backend").resolve("Dockerfile"));

        assertThat(compose)
                .contains("java")
                .contains("Healthcheck")
                .doesNotContain("wget")
                .doesNotContain("curl")
                .doesNotContain("nc ");
        assertThat(backendDockerfile)
                .contains("Healthcheck.java")
                .contains("javac")
                .contains("Healthcheck.class");
    }
}
