package com.fan.mixmyfit.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DeploymentSecurityConfigTest {
    private final Path projectRoot = Path.of("").toAbsolutePath().normalize().getParent();

    @Test
    void sessionCookieCanUseSameSiteNoneForVercelToRailwayDeployment() {
        SessionCookieFactory cookieFactory = new SessionCookieFactory(true, "None");

        String setCookie = cookieFactory.createSessionCookie("session-id").toString();

        assertThat(setCookie)
                .contains("MMF_SESSION=session-id")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("SameSite=None");
    }

    @Test
    void backendDefinesCorsConfigurationForConfiguredFrontendOrigin() throws IOException {
        Path corsConfig = projectRoot.resolve("backend")
                .resolve("src")
                .resolve("main")
                .resolve("java")
                .resolve("com")
                .resolve("fan")
                .resolve("mixmyfit")
                .resolve("security")
                .resolve("CorsConfig.java");
        String application = Files.readString(projectRoot.resolve("backend")
                .resolve("src")
                .resolve("main")
                .resolve("resources")
                .resolve("application.yml"));

        assertThat(corsConfig).exists();
        assertThat(Files.readString(corsConfig))
                .contains("WebMvcConfigurer")
                .contains("allowedOrigins")
                .contains("allowCredentials(true)")
                .contains("/api/**");
        assertThat(application)
                .contains("allowed-origins: ${MIXMYFIT_CORS_ALLOWED_ORIGINS:}")
                .contains("cookie-same-site: ${MIXMYFIT_AUTH_COOKIE_SAME_SITE:Lax}");
    }
}
