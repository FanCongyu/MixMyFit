package com.fan.mixmyfit.category;

import static org.assertj.core.api.Assertions.assertThat;

import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.support.AuthenticatedUserFixture;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, CategoryEndpointTest.TestDatabaseConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryEndpointTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    private AuthenticatedUserFixture fixture;

    @BeforeEach
    void setUp() {
        restTemplate.getRestTemplate().setRequestFactory(new JdkClientHttpRequestFactory());
        fixture = new AuthenticatedUserFixture(restTemplate, users);
    }

    @Test
    void loggedInUserCanSeeFixedCategories() {
        var user = fixture.createLoggedInUser("category-fixed");

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/categories",
                HttpMethod.GET,
                new HttpEntity<>(user.sessionHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("\"name\":\"上装\"");
        assertThat(response.getBody()).contains("\"name\":\"下装\"");
        assertThat(response.getBody()).contains("\"name\":\"鞋子\"");
        assertThat(response.getBody()).contains("\"name\":\"帽子\"");
        assertThat(response.getBody()).contains("\"type\":\"fixed\"");
    }

    @Test
    void userCanCreateCustomCategory() {
        var user = fixture.createLoggedInUser("category-create");

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/categories",
                new HttpEntity<>(Map.of("name", "项链"), user.sessionHeaders()),
                String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).contains("\"categoryId\":");
        assertThat(response.getBody()).contains("\"name\":\"项链\"");
        assertThat(response.getBody()).contains("\"type\":\"custom\"");
    }

    @Test
    void duplicateCustomCategoryNameForSameUserIsRejected() {
        var user = fixture.createLoggedInUser("category-duplicate");
        ResponseEntity<String> first = restTemplate.postForEntity(
                "/api/categories",
                new HttpEntity<>(Map.of("name", "包"), user.sessionHeaders()),
                String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> duplicate = restTemplate.postForEntity(
                "/api/categories",
                new HttpEntity<>(Map.of("name", "包"), user.sessionHeaders()),
                String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicate.getBody()).contains("\"code\":\"CATEGORY_NAME_EXISTS\"");
    }

    @Test
    void userCannotViewOrModifyAnotherUsersCustomCategory() {
        var users = fixture.createUserPair();
        ResponseEntity<String> created = restTemplate.postForEntity(
                "/api/categories",
                new HttpEntity<>(Map.of("name", "围巾"), users.userB().sessionHeaders()),
                String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        Long categoryId = JsonIds.extract(created.getBody(), "categoryId");

        ResponseEntity<String> view = restTemplate.exchange(
                "/api/categories/" + categoryId,
                HttpMethod.GET,
                new HttpEntity<>(users.userA().sessionHeaders()),
                String.class);
        ResponseEntity<String> update = restTemplate.exchange(
                "/api/categories/" + categoryId,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", "Updated"), users.userA().sessionHeaders()),
                String.class);

        assertThat(view.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
        assertThat(update.getStatusCode()).isIn(HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND);
    }

    private static final class JsonIds {
        private JsonIds() {
        }

        static Long extract(String json, String fieldName) {
            String marker = "\"" + fieldName + "\":";
            int start = json.indexOf(marker) + marker.length();
            int end = start;
            while (end < json.length() && Character.isDigit(json.charAt(end))) {
                end++;
            }
            return Long.valueOf(json.substring(start, end));
        }
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
