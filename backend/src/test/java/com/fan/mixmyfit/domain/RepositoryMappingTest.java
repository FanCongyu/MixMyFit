package com.fan.mixmyfit.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fan.mixmyfit.MixMyFitApplication;
import com.fan.mixmyfit.domain.repository.CategoryRepository;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.domain.repository.OutfitItemRepository;
import com.fan.mixmyfit.domain.repository.OutfitRepository;
import com.fan.mixmyfit.domain.repository.UserRepository;
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
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@Testcontainers
@SpringBootTest(classes = {MixMyFitApplication.class, RepositoryMappingTest.TestDatabaseConfig.class})
class RepositoryMappingTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36");

    @Autowired
    private UserRepository users;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private ClothingRepository clothes;

    @Autowired
    private ClothingTagRepository clothingTags;

    @Autowired
    private OutfitRepository outfits;

    @Autowired
    private OutfitItemRepository outfitItems;

    @Test
    void savesAndReadsUserFixedCategoryCustomCategoryAndDraftClothing() {
        User user = users.saveAndFlush(new User("mapping-user", "hash", "Mapping User"));
        Category fixedCategory = categories.saveAndFlush(Category.fixed("上装"));
        Category customCategory = categories.saveAndFlush(Category.custom(user, "包"));

        Clothing draft = clothes.saveAndFlush(Clothing.draft(
                user,
                "uploads/user/draft.png",
                "draft.png",
                "image/png",
                1024L));

        assertThat(users.findById(user.getUserId())).contains(user);
        assertThat(categories.findById(fixedCategory.getCategoryId()))
                .get()
                .extracting(Category::getType, Category::getUser)
                .containsExactly(CategoryType.FIXED, null);
        assertThat(categories.findById(customCategory.getCategoryId()))
                .get()
                .extracting(Category::getType, category -> category.getUser().getUserId())
                .containsExactly(CategoryType.CUSTOM, user.getUserId());
        assertThat(clothes.findById(draft.getClothingId()))
                .get()
                .extracting(Clothing::getStatus, Clothing::getCategory)
                .containsExactly(ClothingStatus.DRAFT, null);
    }

    @Test
    void clothingWithNullCategoryCanBeStoredAsDraft() {
        User user = users.saveAndFlush(new User("draft-user", "hash", null));

        Clothing draft = clothes.saveAndFlush(Clothing.draft(
                user,
                "uploads/user/no-category.png",
                "no-category.png",
                "image/webp",
                2048L));

        assertThat(draft.getCategory()).isNull();
        assertThat(draft.getStatus()).isEqualTo(ClothingStatus.DRAFT);
    }

    @Test
    void duplicateClothingTagNameForSameUserIsRejected() {
        User user = users.saveAndFlush(new User("tag-owner", "hash", null));
        clothingTags.saveAndFlush(new ClothingTag(user, "通勤"));

        assertThatThrownBy(() -> clothingTags.saveAndFlush(new ClothingTag(user, "通勤")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void outfitItemReferencingMissingClothingIsRejected() {
        User user = users.saveAndFlush(new User("outfit-owner", "hash", null));
        Outfit outfit = outfits.saveAndFlush(new Outfit(user, "Missing Clothing", null));

        assertThatThrownBy(() -> outfitItems.saveAndFlush(OutfitItem.mainSlot(outfit, user, 999_999L, OutfitSlot.TOP)))
                .isInstanceOf(DataIntegrityViolationException.class);
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
