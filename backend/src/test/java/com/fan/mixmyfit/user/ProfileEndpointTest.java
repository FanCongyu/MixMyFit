package com.fan.mixmyfit.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.fan.mixmyfit.MixMyFitApplication;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, ProfileEndpointTest.TestDatabaseConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProfileEndpointTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void allowPatchRequests() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
    }

    @Test
    void profileReturnsCurrentUsersUsernameAndNickname() {
        register("profile-user", "Secret123!", "Profile User");
        HttpHeaders session = loginSession("profile-user", "Secret123!");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/profile",
                HttpMethod.GET,
                new HttpEntity<>(session),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"userId\":");
        assertThat(response.getBody()).contains("\"username\":\"profile-user\"");
        assertThat(response.getBody()).contains("\"nickname\":\"Profile User\"");
        assertThat(response.getBody()).doesNotContain("password");
    }

    @Test
    void patchProfileOnlyChangesCurrentUsersNickname() {
        register("nickname-user-a", "Secret123!", "User A");
        register("nickname-user-b", "Secret123!", "User B");
        HttpHeaders userA = loginSession("nickname-user-a", "Secret123!");
        HttpHeaders userB = loginSession("nickname-user-b", "Secret123!");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/profile",
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("nickname", "Renamed A"), userA),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"username\":\"nickname-user-a\"");
        assertThat(response.getBody()).contains("\"nickname\":\"Renamed A\"");
        assertThat(getProfile(userB).getBody()).contains("\"nickname\":\"User B\"");
    }

    @Test
    void changingPasswordRejectsWrongOldPassword() {
        register("password-reject-user", "Secret123!", "Password User");
        HttpHeaders session = loginSession("password-reject-user", "Secret123!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/profile/password",
                new HttpEntity<>(
                        Map.of("oldPassword", "Wrong123!", "newPassword", "NewSecret123!"),
                        session),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"code\":");
        assertThat(response.getBody()).doesNotContain("NewSecret123!");
    }

    @Test
    void changingPasswordMakesOldPasswordInvalidForLogin() {
        register("password-change-user", "Secret123!", "Password User");
        HttpHeaders session = loginSession("password-change-user", "Secret123!");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/profile/password",
                new HttpEntity<>(
                        Map.of("oldPassword", "Secret123!", "newPassword", "NewSecret123!"),
                        session),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(login("password-change-user", "Secret123!").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(login("password-change-user", "NewSecret123!").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void anonymousProfileRequestsAreRejected() {
        ResponseEntity<String> response = restTemplate.getForEntity("/api/profile", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":");
    }

    private ResponseEntity<String> getProfile(HttpHeaders session) {
        return restTemplate.exchange(
                "/api/profile",
                HttpMethod.GET,
                new HttpEntity<>(session),
                String.class);
    }

    private void register(String username, String password, String nickname) {
        restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "password", password,
                        "confirmPassword", password,
                        "nickname", nickname),
                String.class);
    }

    private ResponseEntity<String> login(String username, String password) {
        return restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", username, "password", password),
                String.class);
    }

    private HttpHeaders loginSession(String username, String password) {
        String setCookie = login(username, password).getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        return headers;
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
