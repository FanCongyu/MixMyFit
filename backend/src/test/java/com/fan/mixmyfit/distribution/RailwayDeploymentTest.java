package com.fan.mixmyfit.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RailwayDeploymentTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void railwayServiceConfigsUseDockerfilesAndHealthChecks() throws IOException {
        String backendConfig = Files.readString(projectRoot.resolve("backend").resolve("railway.json"));
        String frontendConfig = Files.readString(projectRoot.resolve("frontend").resolve("railway.json"));

        assertThat(backendConfig)
                .contains("\"builder\": \"DOCKERFILE\"")
                .contains("\"healthcheckPath\": \"/actuator/health\"")
                .contains("\"restartPolicyType\": \"ON_FAILURE\"");
        assertThat(frontendConfig)
                .contains("\"builder\": \"DOCKERFILE\"")
                .contains("\"healthcheckPath\": \"/\"")
                .contains("\"restartPolicyType\": \"ON_FAILURE\"");
    }

    @Test
    void frontendRuntimeProxyCanTargetRailwayBackendService() throws IOException {
        String frontendDockerfile = Files.readString(projectRoot.resolve("frontend").resolve("Dockerfile"));

        assertThat(frontendDockerfile)
                .contains("PORT=80")
                .contains("BACKEND_ORIGIN")
                .contains("mkdir -p /etc/nginx/templates")
                .contains("/etc/nginx/templates/default.conf.template")
                .contains("listen ${PORT};")
                .doesNotContain("proxy_pass http://backend:8080/api/");
    }

    @Test
    void backendUsesRailwayInjectedPortWithLocalDefault() throws IOException {
        String application = Files.readString(projectRoot.resolve("backend")
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("application.yml"));

        assertThat(application)
                .contains("server:")
                .contains("port: ${PORT:8080}");
    }

    @Test
    void deploymentSmokeScriptChecksHealthWebUiAndCookieFlags() throws IOException {
        Path smokeScript = projectRoot.resolve("e2e").resolve("railway-smoke.ps1");

        assertThat(smokeScript).exists();
        String smoke = Files.readString(smokeScript);

        assertThat(smoke)
                .contains("MIXMYFIT_WEBUI_URL")
                .contains("MIXMYFIT_HEALTH_URL")
                .contains("MMF_SESSION")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=Lax");
    }

    @Test
    void deploymentFilesDoNotCommitProductionSecrets() throws IOException {
        String deploymentText = Files.readString(projectRoot.resolve("backend").resolve("railway.json"))
                + Files.readString(projectRoot.resolve("frontend").resolve("railway.json"));

        assertThat(deploymentText)
                .doesNotContain("MYSQL_PASSWORD")
                .doesNotContain("SPRING_DATASOURCE_PASSWORD")
                .doesNotContain("RAILWAY_TOKEN")
                .doesNotContain("mixmyfit_dev_password");
    }
}
