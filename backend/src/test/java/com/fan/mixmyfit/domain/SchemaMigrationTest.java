package com.fan.mixmyfit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fan.mixmyfit.MixMyFitApplication;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(classes = {MixMyFitApplication.class, SchemaMigrationTest.TestDatabaseConfig.class})
class SchemaMigrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    private static final Set<String> REQUIRED_TABLES = Set.of(
            "users",
            "categories",
            "clothes",
            "clothing_seasons",
            "clothing_tags",
            "clothing_tag_links",
            "outfit_tags",
            "outfits",
            "outfit_seasons",
            "outfit_tag_links",
            "outfit_items");

    @Autowired
    private DataSource dataSource;

    @Test
    void flywayCreatesAllRequiredTables() throws SQLException {
        assertThat(tableNames()).containsAll(REQUIRED_TABLES);
        assertThat(tableNames()).contains("flyway_schema_history");
    }

    @Test
    void requiredTablesExposeExpectedColumns() throws SQLException {
        Map<String, Set<String>> expectedColumns = new LinkedHashMap<>();
        expectedColumns.put("users", Set.of("user_id", "username", "password_hash", "nickname", "created_at", "updated_at"));
        expectedColumns.put("categories", Set.of("category_id", "user_id", "name", "type", "created_at", "updated_at"));
        expectedColumns.put("clothes", Set.of("clothing_id", "user_id", "category_id", "name", "color", "image_path", "original_filename", "content_type", "file_size", "status", "created_at", "updated_at"));
        expectedColumns.put("clothing_seasons", Set.of("clothing_season_id", "clothing_id", "season"));
        expectedColumns.put("clothing_tags", Set.of("clothing_tag_id", "user_id", "name", "created_at", "updated_at"));
        expectedColumns.put("clothing_tag_links", Set.of("clothing_tag_link_id", "clothing_id", "clothing_tag_id"));
        expectedColumns.put("outfit_tags", Set.of("outfit_tag_id", "user_id", "name", "created_at", "updated_at"));
        expectedColumns.put("outfits", Set.of("outfit_id", "user_id", "title", "note", "created_at", "updated_at"));
        expectedColumns.put("outfit_seasons", Set.of("outfit_season_id", "outfit_id", "season"));
        expectedColumns.put("outfit_tag_links", Set.of("outfit_tag_link_id", "outfit_id", "outfit_tag_id"));
        expectedColumns.put("outfit_items", Set.of("outfit_item_id", "outfit_id", "user_id", "clothing_id", "role", "slot", "position_x", "position_y", "size", "z_index"));

        for (var entry : expectedColumns.entrySet()) {
            assertThat(columnNames(entry.getKey()))
                    .as("columns for table %s", entry.getKey())
                    .containsAll(entry.getValue());
        }
    }

    @Test
    void usernameIsGloballyUnique() throws SQLException {
        insertUser("unique-user", "hash-one");

        assertThatThrownBy(() -> insertUser("unique-user", "hash-two"))
                .isInstanceOf(SQLException.class);
    }

    @Test
    void customCategoryNamesAreUniquePerUser() throws SQLException {
        long userOne = insertUser("category-user-one", "hash");
        long userTwo = insertUser("category-user-two", "hash");
        insertCategory(userOne, "包", "custom");

        assertThatThrownBy(() -> insertCategory(userOne, "包", "custom"))
                .isInstanceOf(SQLException.class);

        insertCategory(userTwo, "包", "custom");
    }

    @Test
    void tagNamesAreUniquePerUserAndTagDomain() throws SQLException {
        long userOne = insertUser("tag-user-one", "hash");
        long userTwo = insertUser("tag-user-two", "hash");
        insertClothingTag(userOne, "通勤");
        insertOutfitTag(userOne, "通勤");

        assertThatThrownBy(() -> insertClothingTag(userOne, "通勤"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> insertOutfitTag(userOne, "通勤"))
                .isInstanceOf(SQLException.class);

        insertClothingTag(userTwo, "通勤");
        insertOutfitTag(userTwo, "通勤");
    }

    @Test
    void enumLikeColumnsRejectInvalidValues() throws SQLException {
        long userId = insertUser("enum-user", "hash");
        long topCategory = insertCategory(null, "枚举测试上装", "fixed");
        long clothingId = insertReadyClothing(userId, topCategory, "enum-top.png");

        assertThatThrownBy(() -> insertCategory(userId, "Invalid", "unexpected"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute("insert into clothes (user_id, category_id, image_path, original_filename, content_type, file_size, status) values (?, ?, ?, ?, ?, ?, ?)",
                userId, topCategory, "/uploads/bad.png", "bad.png", "image/png", 1L, "archived"))
                .isInstanceOf(SQLException.class);
        assertThatThrownBy(() -> execute("insert into clothing_seasons (clothing_id, season) values (?, ?)", clothingId, "monsoon"))
                .isInstanceOf(SQLException.class);
        assertThatCode(() -> insertOutfit(userId, "Valid Outfit"))
                .doesNotThrowAnyException();
    }

    @Test
    void outfitItemsMustReferenceClothingOwnedByTheSameUser() throws SQLException {
        long userOne = insertUser("owner-one", "hash");
        long userTwo = insertUser("owner-two", "hash");
        long topCategory = insertCategory(null, "上装", "fixed");
        long userOneClothing = insertReadyClothing(userOne, topCategory, "owner-one-top.png");
        long userTwoOutfit = insertOutfit(userTwo, "Other User Outfit");

        assertThatThrownBy(() -> insertMainSlotOutfitItem(userTwoOutfit, userTwo, userOneClothing, "top"))
                .isInstanceOf(SQLException.class);
    }

    private Set<String> tableNames() throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        select table_name
                        from information_schema.tables
                        where table_schema = database()
                        """);
                ResultSet resultSet = statement.executeQuery()) {
            return resultSetToSet(resultSet, "table_name");
        }
    }

    private Set<String> columnNames(String tableName) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement("""
                        select column_name
                        from information_schema.columns
                        where table_schema = database()
                          and table_name = ?
                        """)) {
            statement.setString(1, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSetToSet(resultSet, "column_name");
            }
        }
    }

    private Set<String> resultSetToSet(ResultSet resultSet, String column) throws SQLException {
        Set<String> values = new java.util.LinkedHashSet<>();
        while (resultSet.next()) {
            values.add(resultSet.getString(column));
        }
        return values;
    }

    private long insertUser(String username, String passwordHash) throws SQLException {
        return insertAndReturnId(
                "insert into users (username, password_hash, nickname) values (?, ?, ?)",
                username,
                passwordHash,
                username + "-nick");
    }

    private long insertCategory(Long userId, String name, String type) throws SQLException {
        return insertAndReturnId(
                "insert into categories (user_id, name, type) values (?, ?, ?)",
                userId,
                name,
                type);
    }

    private long insertClothingTag(long userId, String name) throws SQLException {
        return insertAndReturnId(
                "insert into clothing_tags (user_id, name) values (?, ?)",
                userId,
                name);
    }

    private long insertOutfitTag(long userId, String name) throws SQLException {
        return insertAndReturnId(
                "insert into outfit_tags (user_id, name) values (?, ?)",
                userId,
                name);
    }

    private long insertReadyClothing(long userId, long categoryId, String filename) throws SQLException {
        return insertAndReturnId(
                """
                        insert into clothes
                            (user_id, category_id, name, color, image_path, original_filename, content_type, file_size, status)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                categoryId,
                "Top",
                "white",
                "/uploads/" + filename,
                filename,
                "image/png",
                1024L,
                "ready");
    }

    private long insertOutfit(long userId, String title) throws SQLException {
        return insertAndReturnId(
                "insert into outfits (user_id, title, note) values (?, ?, ?)",
                userId,
                title,
                null);
    }

    private long insertMainSlotOutfitItem(long outfitId, long userId, long clothingId, String slot) throws SQLException {
        return insertAndReturnId(
                """
                        insert into outfit_items
                            (outfit_id, user_id, clothing_id, role, slot)
                        values (?, ?, ?, ?, ?)
                        """,
                outfitId,
                userId,
                clothingId,
                "main_slot",
                slot);
    }

    private long insertAndReturnId(String sql, Object... args) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, args);
            statement.executeUpdate();
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                assertThat(generatedKeys.next()).isTrue();
                return generatedKeys.getLong(1);
            }
        }
    }

    private void execute(String sql, Object... args) throws SQLException {
        try (Connection connection = dataSource.getConnection();
                PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, args);
            statement.executeUpdate();
        }
    }

    private void bind(PreparedStatement statement, Object... args) throws SQLException {
        for (int index = 0; index < args.length; index++) {
            statement.setObject(index + 1, args[index]);
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
