package com.fan.mixmyfit.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.OutfitRepository;
import com.fan.mixmyfit.domain.repository.UserRepository;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(
        classes = {MixMyFitApplication.class, CoreFlowE2eTest.TestDatabaseConfig.class},
        properties = "UPLOAD_DIR=build/e2e-uploads",
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoreFlowE2eTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UserRepository users;

    @Autowired
    private ClothingRepository clothes;

    @Autowired
    private OutfitRepository outfits;

    @Test
    void userCompletesCoreFlowFromRegistrationToTaggedOutfitFiltering() throws Exception {
        UserSession user = registerAndLogin("core-flow-user");
        long categoryId = createCategory(user, "Layer");
        long clothingTagId = createTag(user, "/api/clothing-tags", "Office clothing");
        long outfitTagId = createTag(user, "/api/outfit-tags", "Office outfit");

        long topId = uploadImage(user, "top.png");
        long shoesId = uploadImage(user, "shoes.png");

        JsonNode batch = postJson(
                "/api/clothes/batch",
                user.headers(),
                Map.of(
                        "clothingIds", List.of(topId, shoesId),
                        "categoryId", categoryId,
                        "color", "navy",
                        "seasons", List.of("spring", "summer"),
                        "addTagIds", List.of(clothingTagId)),
                HttpStatus.OK);
        assertThat(batch.path("updated").asInt()).isEqualTo(2);

        JsonNode readyClothes = getJson(
                "/api/clothes?status=ready&season=summer&tagIds=" + clothingTagId,
                user.headers(),
                HttpStatus.OK);
        assertThat(readyClothes.path("total").asInt()).isEqualTo(2);

        JsonNode createdOutfit = postJson(
                "/api/outfits",
                user.headers(),
                Map.of(
                        "title", "Office set",
                        "seasons", List.of("summer"),
                        "tagIds", List.of(outfitTagId),
                        "items", List.of(
                                Map.of("clothingId", topId, "role", "main_slot", "slot", "top"),
                                Map.of("clothingId", shoesId, "role", "main_slot", "slot", "shoes"))),
                HttpStatus.CREATED);

        JsonNode filteredOutfits = getJson(
                "/api/outfits?tagIds=" + outfitTagId,
                user.headers(),
                HttpStatus.OK);
        assertThat(filteredOutfits.path("total").asInt()).isEqualTo(1);
        assertThat(filteredOutfits.path("items").get(0).path("outfitId").asLong())
                .isEqualTo(createdOutfit.path("outfitId").asLong());
        assertThat(filteredOutfits.path("items").get(0).path("title").asText()).isEqualTo("Office set");
    }

    @Test
    void userCannotDirectlyAccessAnotherUsersClothingOrOutfit() throws Exception {
        UserSession userA = registerAndLogin("isolation-a");
        UserSession userB = registerAndLogin("isolation-b");

        long categoryId = createCategory(userB, "Private Layer");
        long outfitTagId = createTag(userB, "/api/outfit-tags", "Private outfit");
        long privateClothingId = uploadImage(userB, "private.png");
        postJson(
                "/api/clothes/batch",
                userB.headers(),
                Map.of(
                        "clothingIds", List.of(privateClothingId),
                        "categoryId", categoryId),
                HttpStatus.OK);
        long privateOutfitId = postJson(
                "/api/outfits",
                userB.headers(),
                Map.of(
                        "title", "Private set",
                        "tagIds", List.of(outfitTagId),
                        "items", List.of(Map.of(
                                "clothingId", privateClothingId,
                                "role", "main_slot",
                                "slot", "top"))),
                HttpStatus.CREATED)
                .path("outfitId")
                .asLong();

        getJson("/api/clothes/" + privateClothingId, userA.headers(), HttpStatus.NOT_FOUND);
        getJson("/api/outfits/" + privateOutfitId, userA.headers(), HttpStatus.NOT_FOUND);

        assertThat(clothes.findById(privateClothingId)).isPresent();
        assertThat(outfits.findById(privateOutfitId)).isPresent();
    }

    private UserSession registerAndLogin(String username) {
        ResponseEntity<String> registration = restTemplate.postForEntity(
                "/api/auth/register",
                Map.of(
                        "username", username,
                        "password", "Secret123!",
                        "confirmPassword", "Secret123!",
                        "nickname", username),
                String.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<String> login = restTemplate.postForEntity(
                "/api/auth/login",
                Map.of("username", username, "password", "Secret123!"),
                String.class);
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        String setCookie = login.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(setCookie).startsWith("MMF_SESSION=");

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.COOKIE, setCookie.split(";", 2)[0]);
        return new UserSession(users.findByUsername(username).orElseThrow().getUserId(), headers);
    }

    private long createCategory(UserSession user, String name) throws Exception {
        return postJson("/api/categories", user.headers(), Map.of("name", name), HttpStatus.CREATED)
                .path("categoryId")
                .asLong();
    }

    private long createTag(UserSession user, String path, String name) throws Exception {
        return postJson(path, user.headers(), Map.of("name", name), HttpStatus.CREATED)
                .path("tagId")
                .asLong();
    }

    private long uploadImage(UserSession user, String filename) throws Exception {
        HttpHeaders headers = copyHeaders(user.headers());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new NamedByteArrayResource(tinyPng(), filename));

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/clothes",
                new HttpEntity<>(body, headers),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return objectMapper.readTree(response.getBody()).path("clothingId").asLong();
    }

    private JsonNode getJson(String path, HttpHeaders headers, HttpStatus expectedStatus) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                path,
                HttpMethod.GET,
                new HttpEntity<>(copyHeaders(headers)),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return parseBody(response);
    }

    private JsonNode postJson(String path, HttpHeaders headers, Object body, HttpStatus expectedStatus) throws Exception {
        HttpHeaders requestHeaders = copyHeaders(headers);
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<String> response = restTemplate.postForEntity(
                path,
                new HttpEntity<>(body, requestHeaders),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        return parseBody(response);
    }

    private JsonNode parseBody(ResponseEntity<String> response) throws Exception {
        if (response.getBody() == null || response.getBody().isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(response.getBody());
    }

    private static HttpHeaders copyHeaders(HttpHeaders source) {
        HttpHeaders copy = new HttpHeaders();
        copy.addAll(source);
        return copy;
    }

    private static byte[] tinyPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private record UserSession(Long userId, HttpHeaders headers) {
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
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
