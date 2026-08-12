package com.fan.mixmyfit.tag;

import static org.assertj.core.api.Assertions.assertThat;

import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagRepository;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, TagEndpointTest.TestDatabaseConfig.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TagEndpointTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private ClothingTagRepository clothingTags;

    @Autowired
    private OutfitTagRepository outfitTags;

    private AuthenticatedUserFixture fixture;

    @BeforeEach
    void setUp() {
        fixture = new AuthenticatedUserFixture(restTemplate, users);
    }

    @Test
    void sameTextClothingTagAndOutfitTagAreStoredSeparately() {
        var user = fixture.createLoggedInUser("tag-separate");

        ResponseEntity<String> clothing = restTemplate.postForEntity(
                "/api/clothing-tags",
                new HttpEntity<>(Map.of("name", "通勤"), user.sessionHeaders()),
                String.class);
        ResponseEntity<String> outfit = restTemplate.postForEntity(
                "/api/outfit-tags",
                new HttpEntity<>(Map.of("name", "通勤"), user.sessionHeaders()),
                String.class);

        assertThat(clothing.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(outfit.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(clothing.getBody()).contains("\"name\":\"通勤\"");
        assertThat(outfit.getBody()).contains("\"name\":\"通勤\"");
        assertThat(clothing.getBody()).contains("\"kind\":\"clothing\"");
        assertThat(outfit.getBody()).contains("\"kind\":\"outfit\"");
        assertThat(clothingTags.count()).isEqualTo(1);
        assertThat(outfitTags.count()).isEqualTo(1);
    }

    @Test
    void duplicateTagNameForSameUserAndKindIsRejected() {
        var user = fixture.createLoggedInUser("tag-duplicate");
        ResponseEntity<String> first = restTemplate.postForEntity(
                "/api/clothing-tags",
                new HttpEntity<>(Map.of("name", "牛仔"), user.sessionHeaders()),
                String.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> duplicate = restTemplate.postForEntity(
                "/api/clothing-tags",
                new HttpEntity<>(Map.of("name", "牛仔"), user.sessionHeaders()),
                String.class);

        assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(duplicate.getBody()).contains("\"code\":\"TAG_NAME_EXISTS\"");
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
