package com.fan.mixmyfit.distribution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class RailwayDeploymentTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void railwayBackendServiceConfigUsesDockerfileAndHealthCheck() throws IOException {
        String backendConfig = Files.readString(projectRoot.resolve("backend").resolve("railway.json"));

        assertThat(backendConfig)
                .contains("\"builder\": \"DOCKERFILE\"")
                .contains("\"healthcheckPath\": \"/api/health\"")
                .contains("\"restartPolicyType\": \"ON_FAILURE\"");
    }

    @Test
    void frontendDeploymentUsesVercelApiBaseUrlInsteadOfRailwayPrivateNetworking() throws IOException {
        String frontendDockerfile = Files.readString(projectRoot.resolve("frontend").resolve("Dockerfile"));
        Path frontendRailwayConfig = projectRoot.resolve("frontend").resolve("railway.json");
        Path frontendEnvExample = projectRoot.resolve("frontend").resolve(".env.example");

        assertThat(frontendDockerfile)
                .doesNotContain("backend.railway.internal")
                .doesNotContain("RAILWAY_PRIVATE_DOMAIN")
                .doesNotContain("BACKEND_ORIGIN");
        assertThat(frontendRailwayConfig).doesNotExist();
        assertThat(frontendEnvExample).exists();
        assertThat(Files.readString(frontendEnvExample))
                .contains("VITE_API_BASE_URL=your_backend_public_url_here");
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
                .contains("port: ${PORT:8080}")
                .contains("address: 0.0.0.0");
    }

    @Test
    void deploymentSmokeScriptChecksRailwayBackendAndVercelWebUi() throws IOException {
        Path smokeScript = projectRoot.resolve("e2e").resolve("railway-smoke.ps1");

        assertThat(smokeScript).exists();
        String smoke = Files.readString(smokeScript);

        assertThat(smoke)
                .contains("MIXMYFIT_WEBUI_URL")
                .contains("MIXMYFIT_API_BASE_URL")
                .contains("MIXMYFIT_HEALTH_URL")
                .contains("MMF_SESSION")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None");
    }

    @Test
    void docsDescribeVercelFrontendAndRailwayBackendDeployment() throws IOException {
        String readme = Files.readString(projectRoot.resolve("README.md"));
        String plan = Files.readString(projectRoot.resolve("PLAN.md"));

        assertThat(readme)
                .contains("前端部署到 Vercel")
                .contains("后端部署到 Railway")
                .contains("VITE_API_BASE_URL")
                .contains("Railway Public Domain")
                .contains("线上 WebUI URL：`https://mix-my-fit.vercel.app/`")
                .contains("MIXMYFIT_CORS_ALLOWED_ORIGINS=https://mix-my-fit.vercel.app")
                .doesNotContain("MIXMYFIT_CORS_ALLOWED_ORIGINS=https://mix-my-fit.vercel.app/")
                .doesNotContain("公网 WebUI URL 尚需")
                .doesNotContain("公网 WebUI URL 仍需")
                .doesNotContain("frontend/railway.json")
                .contains("Railway Healthcheck Path：`/api/health`")
                .contains("Railway 变量值不要带双引号")
                .contains("RAILWAY_HEALTHCHECK_TIMEOUT_SEC=300")
                .doesNotContain("backend.railway.internal")
                .doesNotContain("RAILWAY_PRIVATE_DOMAIN");
        assertThat(plan)
                .contains("Vercel + Railway")
                .contains("VITE_API_BASE_URL")
                .doesNotContain("前端部署 Railway");
    }

    @Test
    void deploymentFilesDoNotCommitProductionSecrets() throws IOException {
        String deploymentText = Files.readString(projectRoot.resolve("backend").resolve("railway.json"))
                + Files.readString(projectRoot.resolve("frontend").resolve("vercel.json"))
                + Files.readString(projectRoot.resolve("frontend").resolve(".env.example"));

        assertThat(deploymentText)
                .doesNotContain("MYSQL_PASSWORD")
                .doesNotContain("SPRING_DATASOURCE_PASSWORD")
                .doesNotContain("RAILWAY_TOKEN")
                .doesNotContain("mixmyfit_dev_password");
    }
}
