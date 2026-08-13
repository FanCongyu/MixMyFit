package com.fan.mixmyfit.clothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.ClothingSeason;
import com.fan.mixmyfit.domain.ClothingTag;
import com.fan.mixmyfit.domain.ClothingTagLink;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.CategoryRepository;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.ClothingSeasonRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagLinkRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.file.FileExceptionHandler;
import com.fan.mixmyfit.file.StoredFileService;
import com.fan.mixmyfit.security.CurrentUserResolver;
import com.fan.mixmyfit.security.SecurityExceptionHandler;
import com.fan.mixmyfit.security.SessionCookieFactory;
import com.fan.mixmyfit.security.SessionRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ClothingCrudEndpointTest {
    private static final String SESSION_ID = "session-1";

    private final User owner = user(1L, "owner");
    private final User anotherUser = user(2L, "another");
    private final List<Clothing> clothes = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private final List<ClothingSeason> seasons = new ArrayList<>();
    private final List<ClothingTag> tags = new ArrayList<>();
    private final List<ClothingTagLink> tagLinks = new ArrayList<>();

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        CurrentUserResolver currentUsers = new CurrentUserResolver((SessionRegistry) null, (UserRepository) null) {
            @Override
            public User requireUser(String sessionId) {
                return owner;
            }

            @Override
            public Long requireUserId(String sessionId) {
                return owner.getUserId();
            }
        };

        ClothingService service = new ClothingService(
                currentUsers,
                provider(clothingRepository()),
                new StoredFileService("build/test-uploads"),
                provider(categoryRepository()),
                provider(clothingSeasonRepository()),
                provider(clothingTagRepository()),
                provider(clothingTagLinkRepository()));

        mvc = MockMvcBuilders.standaloneSetup(new ClothingController(service))
                .setControllerAdvice(new FileExceptionHandler(), new SecurityExceptionHandler())
                .build();
    }

    @Test
    void userCanReadOwnClothing() throws Exception {
        Clothing clothing = saveClothing(Clothing.draft(owner, "owner.png", "shirt.png", "image/png", 8L), 100L);

        mvc.perform(get("/api/clothes/100").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clothingId").value(100))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.imageUrl").value("/api/clothes/100/image"))
                .andExpect(jsonPath("$.originalFilename").value("shirt.png"))
                .andExpect(jsonPath("$.fileSize").value(8));

        assertThat(clothing.getUser()).isEqualTo(owner);
    }

    @Test
    void userCanUpdateClothingMetadata() throws Exception {
        saveClothing(Clothing.draft(owner, "owner.png", "shirt.png", "image/png", 8L), 101L);
        saveCategory(Category.custom(owner, "Layer"), 200L);
        saveTag(new ClothingTag(owner, "Office"), 300L);

        mvc.perform(patch("/api/clothes/101")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Blue Shirt",
                                  "categoryId": 200,
                                  "color": "blue",
                                  "seasons": ["spring", "summer"],
                                  "tagIds": [300]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clothingId").value(101))
                .andExpect(jsonPath("$.name").value("Blue Shirt"))
                .andExpect(jsonPath("$.color").value("blue"))
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.category.categoryId").value(200))
                .andExpect(jsonPath("$.category.name").value("Layer"))
                .andExpect(jsonPath("$.seasons[0]").value("spring"))
                .andExpect(jsonPath("$.seasons[1]").value("summer"))
                .andExpect(jsonPath("$.tags[0].tagId").value(300))
                .andExpect(jsonPath("$.tags[0].name").value("Office"));
    }

    @Test
    void patchOnlyUpdatesProvidedMetadataFields() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 202L);
        saveClothing(Clothing.ready(owner, category, "Blue Shirt", "blue", "owner.png", "shirt.png", "image/png", 8L), 105L);

        mvc.perform(patch("/api/clothes/105")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"color\":\"navy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Blue Shirt"))
                .andExpect(jsonPath("$.color").value("navy"))
                .andExpect(jsonPath("$.status").value("ready"))
                .andExpect(jsonPath("$.category.categoryId").value(202));
    }

    @Test
    void userCannotReadUpdateOrDeleteAnotherUsersClothing() throws Exception {
        saveClothing(Clothing.draft(anotherUser, "other.png", "other.png", "image/png", 8L), 102L);

        mvc.perform(get("/api/clothes/102").cookie(sessionCookie()))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/clothes/102")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Stolen\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/clothes/102").cookie(sessionCookie()))
                .andExpect(status().isNotFound());
    }

    @Test
    void userCannotAssignAnotherUsersCustomCategoryToOwnClothing() throws Exception {
        saveClothing(Clothing.draft(owner, "owner.png", "shirt.png", "image/png", 8L), 103L);
        saveCategory(Category.custom(anotherUser, "Private"), 201L);

        mvc.perform(patch("/api/clothes/103")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":201}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void userCanDeleteOwnClothing() throws Exception {
        saveClothing(Clothing.draft(owner, "owner.png", "shirt.png", "image/png", 8L), 104L);

        mvc.perform(delete("/api/clothes/104").cookie(sessionCookie()))
                .andExpect(status().isNoContent());
        assertThat(clothes).isEmpty();
    }

    private Clothing saveClothing(Clothing clothing, Long clothingId) {
        setId(clothing, "clothingId", clothingId);
        clothes.add(clothing);
        return clothing;
    }

    private Category saveCategory(Category category, Long categoryId) {
        setId(category, "categoryId", categoryId);
        categories.add(category);
        return category;
    }

    private ClothingTag saveTag(ClothingTag tag, Long tagId) {
        setId(tag, "clothingTagId", tagId);
        tags.add(tag);
        return tag;
    }

    private ClothingRepository clothingRepository() {
        return (ClothingRepository) java.lang.reflect.Proxy.newProxyInstance(
                ClothingRepository.class.getClassLoader(),
                new Class<?>[] { ClothingRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> clothes.stream()
                            .filter(clothing -> clothing.getClothingId().equals(args[0]))
                            .findFirst();
                    case "findByUserUserIdOrderByClothingId" -> clothes.stream()
                            .filter(clothing -> clothing.getUser().getUserId().equals(args[0]))
                            .sorted(Comparator.comparing(Clothing::getClothingId))
                            .toList();
                    case "save" -> args[0];
                    case "delete" -> clothes.remove(args[0]);
                    case "toString" -> "FakeClothingRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private CategoryRepository categoryRepository() {
        return (CategoryRepository) java.lang.reflect.Proxy.newProxyInstance(
                CategoryRepository.class.getClassLoader(),
                new Class<?>[] { CategoryRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> categories.stream()
                            .filter(category -> category.getCategoryId().equals(args[0]))
                            .findFirst();
                    case "toString" -> "FakeCategoryRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ClothingSeasonRepository clothingSeasonRepository() {
        return (ClothingSeasonRepository) java.lang.reflect.Proxy.newProxyInstance(
                ClothingSeasonRepository.class.getClassLoader(),
                new Class<?>[] { ClothingSeasonRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByClothingClothingIdOrderByClothingSeasonId" -> seasons.stream()
                            .filter(season -> season.getClothing().getClothingId().equals(args[0]))
                            .toList();
                    case "deleteByClothingClothingId" -> {
                        seasons.removeIf(season -> season.getClothing().getClothingId().equals(args[0]));
                        yield null;
                    }
                    case "saveAll" -> {
                        for (Object season : (Iterable<?>) args[0]) {
                            seasons.add((ClothingSeason) season);
                        }
                        yield args[0];
                    }
                    case "toString" -> "FakeClothingSeasonRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ClothingTagRepository clothingTagRepository() {
        return (ClothingTagRepository) java.lang.reflect.Proxy.newProxyInstance(
                ClothingTagRepository.class.getClassLoader(),
                new Class<?>[] { ClothingTagRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> tags.stream()
                            .filter(tag -> tag.getClothingTagId().equals(args[0]))
                            .findFirst();
                    case "toString" -> "FakeClothingTagRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ClothingTagLinkRepository clothingTagLinkRepository() {
        return (ClothingTagLinkRepository) java.lang.reflect.Proxy.newProxyInstance(
                ClothingTagLinkRepository.class.getClassLoader(),
                new Class<?>[] { ClothingTagLinkRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findByClothingClothingIdOrderByClothingTagLinkId" -> tagLinks.stream()
                            .filter(link -> link.getClothing().getClothingId().equals(args[0]))
                            .toList();
                    case "deleteByClothingClothingId" -> {
                        tagLinks.removeIf(link -> link.getClothing().getClothingId().equals(args[0]));
                        yield null;
                    }
                    case "saveAll" -> {
                        for (Object link : (Iterable<?>) args[0]) {
                            tagLinks.add((ClothingTagLink) link);
                        }
                        yield args[0];
                    }
                    case "toString" -> "FakeClothingTagLinkRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private static jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie(SessionCookieFactory.SESSION_COOKIE_NAME, SESSION_ID);
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }

            @Override
            public T getObject() {
                return value;
            }
        };
    }

    private static User user(Long userId, String username) {
        User user = new User(username, "hash", username);
        setId(user, "userId", userId);
        return user;
    }

    private static void setId(Object target, String fieldName, Long id) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
