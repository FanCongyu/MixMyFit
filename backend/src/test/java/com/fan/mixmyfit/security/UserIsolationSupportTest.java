package com.fan.mixmyfit.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.support.AuthenticatedUserFixture;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, UserIsolationSupportTest.TestDatabaseConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserIsolationSupportTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private ClothingRepository clothes;

    @Autowired
    private CurrentUserResolver currentUsers;

    @Autowired
    private OwnershipGuard ownership;

    @Test
    void anonymousProfileRequestReturnsUnauthorized() {
        ResponseEntity<String> response = restTemplate.exchange(
                "/api/profile",
                HttpMethod.GET,
                null,
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).contains("\"code\":\"AUTHENTICATION_REQUIRED\"");
    }

    @Test
    void fixtureCreatesTwoLoggedInUsersWithIndependentCookies() {
        var pair = new AuthenticatedUserFixture(restTemplate, users).createUserPair();

        assertThat(pair.userA().user().getUserId()).isNotEqualTo(pair.userB().user().getUserId());
        assertThat(pair.userA().sessionHeaders().getFirst("Cookie"))
                .startsWith(SessionCookieFactory.SESSION_COOKIE_NAME + "=");
        assertThat(pair.userB().sessionHeaders().getFirst("Cookie"))
                .startsWith(SessionCookieFactory.SESSION_COOKIE_NAME + "=");
        assertThat(pair.userA().sessionHeaders().getFirst("Cookie"))
                .isNotEqualTo(pair.userB().sessionHeaders().getFirst("Cookie"));
    }

    @Test
    void currentUserResolverReturnsTheUserForSessionCookie() {
        var user = new AuthenticatedUserFixture(restTemplate, users).createLoggedInUser("resolver-user");
        String sessionId = user.sessionHeaders()
                .getFirst("Cookie")
                .substring((SessionCookieFactory.SESSION_COOKIE_NAME + "=").length());

        User resolved = currentUsers.requireUser(sessionId);

        assertThat(resolved.getUserId()).isEqualTo(user.user().getUserId());
        assertThat(resolved.getPasswordHash()).isNotBlank();
    }

    @Test
    void ownershipGuardRejectsResourcesOwnedByAnotherUser() {
        var pair = new AuthenticatedUserFixture(restTemplate, users).createUserPair();
        Clothing userBClothing = clothes.saveAndFlush(Clothing.draft(
                pair.userB().user(),
                "uploads/user-b/draft.png",
                "draft.png",
                "image/png",
                1024L));

        assertThatThrownBy(() -> ownership.requireOwner(
                pair.userA().user().getUserId(),
                userBClothing.getUser().getUserId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Resource not found");
    }

    @Test
    void ownershipGuardAllowsCurrentUsersResource() {
        var user = new AuthenticatedUserFixture(restTemplate, users).createLoggedInUser("owner-user");

        ownership.requireOwner(user.user().getUserId(), user.user().getUserId());
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
