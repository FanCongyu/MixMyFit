package com.fan.mixmyfit.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.security.SessionRegistry;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, AuthEndpointTest.TestDatabaseConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthEndpointTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessions;

    @Test
    void registerCreatesUserWithHashedPasswordAndDoesNotExposePasswordHash() {
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", "register-user",
                        "password", "Secret123!",
                        "confirmPassword", "Secret123!",
                        "nickname", "Register User"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"userId\":");
        assertThat(response.getBody()).contains("\"username\":\"register-user\"");
        assertThat(response.getBody()).contains("\"nickname\":\"Register User\"");
        assertThat(response.getBody()).doesNotContain("password");

        var saved = users.findByUsername("register-user").orElseThrow();
        assertThat(saved.getPasswordHash()).isNotEqualTo("Secret123!");
        assertThat(passwordEncoder.matches("Secret123!", saved.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateUsernameRegistrationReturnsSafeError() {
        register("duplicate-user", "Secret123!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", "duplicate-user",
                        "password", "Secret123!",
                        "confirmPassword", "Secret123!"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"code\":");
        assertThat(response.getBody()).contains("\"message\":");
        assertThat(response.getBody()).doesNotContain("password");
    }

    @Test
    void loginSetsHttpOnlySessionCookie() {
        register("login-user", "Secret123!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", "login-user", "password", "Secret123!"),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"userId\":");
        assertThat(response.getBody()).contains("\"username\":\"login-user\"");
        assertThat(response.getBody()).doesNotContain("password");

        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("MMF_SESSION=");
        assertThat(setCookie).contains("Max-Age=604800");
        assertThat(setCookie).contains("HttpOnly");
        assertThat(setCookie).contains("SameSite=Lax");
    }

    @Test
    void invalidLoginDoesNotRevealWhetherUsernameExists() {
        register("known-user", "Secret123!");

        ResponseEntity<String> wrongPassword = login("known-user", "Wrong123!");
        ResponseEntity<String> missingUser = login("missing-user", "Wrong123!");

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(missingUser.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(wrongPassword.getBody()).isEqualTo(missingUser.getBody());
    }

    @Test
    void logoutClearsSessionCookieAndInvalidatesServerSession() {
        register("logout-user", "Secret123!");
        String loginCookie = login("logout-user", "Secret123!")
                .getHeaders()
                .getFirst(HttpHeaders.SET_COOKIE);
        String sessionId = loginCookie.split(";", 2)[0].substring("MMF_SESSION=".length());

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, "MMF_SESSION=" + sessionId);
        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/auth/logout",
                new HttpEntity<>(null, headers),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        String setCookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).contains("MMF_SESSION=");
        assertThat(setCookie).contains("Max-Age=0");
        assertThat(response.getBody()).isNull();
        assertThat(sessions.findUserId(sessionId)).isEmpty();
    }

    private void register(String username, String password) {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of("username", username, "password", password, "confirmPassword", password),
                String.class);
    }

    private ResponseEntity<String> login(String username, String password) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", username, "password", password),
                String.class);
    }

    @TestConfiguration
    static class TestDatabaseConfig {
        @Bean
        DataSource dataSource() {
            return new TestcontainersDataSource(MYSQL);
        }

        @Bean
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
        }

        @Bean
        FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
            return new FlywayMigrationInitializer(flyway);
        }
    }

    private static final class TestcontainersDataSource implements DataSource {
        private final MySQLContainer mysql;

        private TestcontainersDataSource(MySQLContainer mysql) {
            this.mysql = mysql;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return DriverManager.getConnection(mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            return DriverManager.getConnection(mysql.getJdbcUrl(), username, password);
        }

        @Override
        public PrintWriter getLogWriter() {
            return null;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
        }

        @Override
        public void setLoginTimeout(int seconds) {
        }

        @Override
        public int getLoginTimeout() {
            return 0;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getGlobal();
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this);
        }
    }
}
