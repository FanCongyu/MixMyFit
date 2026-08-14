package com.fan.mixmyfit.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CiWiringTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void githubActionsWorkflowDefinesUnitTestJobForBackendAndFrontendChecks() throws IOException {
        Path workflow = projectRoot.resolve(".github").resolve("workflows").resolve("ci.yml");

        assertThat(workflow).exists();
        String ci = Files.readString(workflow);

        assertThat(ci)
                .contains("unit-test:")
                .contains("actions/setup-java")
                .contains("distribution: temurin")
                .contains("java-version: '17'")
                .contains("cd backend && mvn test")
                .contains("actions/setup-node")
                .contains("node-version: '20'")
                .contains("cd frontend && npm ci")
                .contains("cd frontend && npm run build")
                .contains("cd frontend && npm test -- --run");
    }

    @Test
    void makeTestIsLocalOneCommandEntryForBackendAndFrontendChecks() throws IOException {
        String makefile = Files.readString(projectRoot.resolve("Makefile"));

        assertThat(makefile)
                .contains(".PHONY: test")
                .contains("LOCAL_MVN")
                .contains("MVN ?=")
                .contains("test: test-backend test-frontend")
                .contains("cd backend && $(MVN) test")
                .contains("cd frontend && npm ci && npm run build && npm test -- --run");
    }
}
