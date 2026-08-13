package com.fan.mixmyfit.clothing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.LocalDateTime;
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

    @Test
    void draftFilterReturnsClothingMissingCategory() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 210L);
        saveClothing(Clothing.ready(owner, category, "Ready Shirt", "blue", "ready.png", "ready.png", "image/png", 8L), 110L);
        saveClothing(Clothing.draft(owner, "draft.png", "draft.png", "image/png", 8L), 111L);

        mvc.perform(get("/api/clothes").queryParam("status", "draft").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].clothingId").value(111))
                .andExpect(jsonPath("$.items[0].status").value("draft"));
    }

    @Test
    void readyFilterExcludesDraftClothing() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 211L);
        saveClothing(Clothing.draft(owner, "draft.png", "draft.png", "image/png", 8L), 112L);
        saveClothing(Clothing.ready(owner, category, "Ready Shirt", "blue", "ready.png", "ready.png", "image/png", 8L), 113L);

        mvc.perform(get("/api/clothes").queryParam("status", "ready").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].clothingId").value(113))
                .andExpect(jsonPath("$.items[0].status").value("ready"));
    }

    @Test
    void listSupportsPaging() throws Exception {
        saveClothing(Clothing.draft(owner, "first.png", "first.png", "image/png", 8L), 124L);
        saveClothing(Clothing.draft(owner, "second.png", "second.png", "image/png", 8L), 125L);
        saveClothing(Clothing.draft(owner, "third.png", "third.png", "image/png", 8L), 126L);

        mvc.perform(get("/api/clothes")
                        .queryParam("page", "1")
                        .queryParam("size", "1")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].clothingId").value(125));
    }

    @Test
    void listDefaultsToCreatedAtDescending() throws Exception {
        Clothing older = saveClothing(Clothing.draft(owner, "older.png", "older.png", "image/png", 8L), 129L);
        Clothing newer = saveClothing(Clothing.draft(owner, "newer.png", "newer.png", "image/png", 8L), 130L);
        setField(older, "createdAt", LocalDateTime.parse("2026-08-12T10:00:00"));
        setField(newer, "createdAt", LocalDateTime.parse("2026-08-13T10:00:00"));

        mvc.perform(get("/api/clothes").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].clothingId").value(130))
                .andExpect(jsonPath("$.items[1].clothingId").value(129));
    }

    @Test
    void colorSeasonAndTagFiltersOnlyReturnCurrentUsersMatchingClothing() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 212L);
        Category otherCategory = saveCategory(Category.custom(anotherUser, "Private Layer"), 213L);
        ClothingTag office = saveTag(new ClothingTag(owner, "Office"), 310L);
        ClothingTag otherOffice = saveTag(new ClothingTag(anotherUser, "Office"), 311L);

        Clothing matching = saveClothing(Clothing.ready(owner, category, "Navy Shirt", "navy", "match.png", "match.png", "image/png", 8L), 114L);
        seasons.add(new ClothingSeason(matching, com.fan.mixmyfit.domain.Season.SUMMER));
        tagLinks.add(new ClothingTagLink(matching, office));

        Clothing wrongSeason = saveClothing(Clothing.ready(owner, category, "Winter Shirt", "navy", "winter.png", "winter.png", "image/png", 8L), 115L);
        seasons.add(new ClothingSeason(wrongSeason, com.fan.mixmyfit.domain.Season.WINTER));
        tagLinks.add(new ClothingTagLink(wrongSeason, office));

        Clothing otherUsersMatch = saveClothing(Clothing.ready(anotherUser, otherCategory, "Other Shirt", "navy", "other.png", "other.png", "image/png", 8L), 116L);
        seasons.add(new ClothingSeason(otherUsersMatch, com.fan.mixmyfit.domain.Season.SUMMER));
        tagLinks.add(new ClothingTagLink(otherUsersMatch, otherOffice));

        mvc.perform(get("/api/clothes")
                        .queryParam("color", "navy")
                        .queryParam("season", "summer")
                        .queryParam("tagIds", "310")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].clothingId").value(114));
    }

    @Test
    void colorReuseEndpointReturnsOnlyCurrentUsersDistinctColors() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 214L);
        Category otherCategory = saveCategory(Category.custom(anotherUser, "Private Layer"), 215L);
        saveClothing(Clothing.ready(owner, category, "Blue Shirt", "blue", "blue.png", "blue.png", "image/png", 8L), 117L);
        saveClothing(Clothing.ready(owner, category, "Navy Shirt", "navy", "navy.png", "navy.png", "image/png", 8L), 118L);
        saveClothing(Clothing.ready(owner, category, "No Color", null, "none.png", "none.png", "image/png", 8L), 119L);
        saveClothing(Clothing.ready(anotherUser, otherCategory, "Red Shirt", "red", "red.png", "red.png", "image/png", 8L), 120L);

        mvc.perform(get("/api/clothes/colors").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("blue"))
                .andExpect(jsonPath("$[1]").value("navy"))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void draftCountOnlyCountsCurrentUsersDraftClothing() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 216L);
        saveClothing(Clothing.draft(owner, "draft.png", "draft.png", "image/png", 8L), 121L);
        saveClothing(Clothing.ready(owner, category, "Ready Shirt", "blue", "ready.png", "ready.png", "image/png", 8L), 122L);
        saveClothing(Clothing.draft(anotherUser, "other.png", "other.png", "image/png", 8L), 123L);

        mvc.perform(get("/api/clothes/draft-count").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void batchSettingCategoryMakesSelectedDraftClothingReady() throws Exception {
        Clothing first = saveClothing(Clothing.draft(owner, "first.png", "first.png", "image/png", 8L), 131L);
        Clothing second = saveClothing(Clothing.draft(owner, "second.png", "second.png", "image/png", 8L), 132L);
        Category category = saveCategory(Category.custom(owner, "Layer"), 217L);

        mvc.perform(post("/api/clothes/batch")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clothingIds": [131, 132],
                                  "categoryId": 217
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(first.getCategory()).isEqualTo(category);
        assertThat(first.getStatus().dbValue()).isEqualTo("ready");
        assertThat(second.getCategory()).isEqualTo(category);
        assertThat(second.getStatus().dbValue()).isEqualTo("ready");
    }

    @Test
    void batchSettingColorOnlyAffectsSelectedCurrentUserClothing() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 221L);
        Clothing first = saveClothing(Clothing.ready(owner, category, "First", "blue", "first.png", "first.png", "image/png", 8L), 141L);
        Clothing second = saveClothing(Clothing.ready(owner, category, "Second", "gray", "second.png", "second.png", "image/png", 8L), 142L);
        Clothing unselected = saveClothing(Clothing.ready(owner, category, "Third", "green", "third.png", "third.png", "image/png", 8L), 143L);

        mvc.perform(post("/api/clothes/batch")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clothingIds": [141, 142],
                                  "color": "navy"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(first.getColor()).isEqualTo("navy");
        assertThat(second.getColor()).isEqualTo("navy");
        assertThat(unselected.getColor()).isEqualTo("green");
    }

    @Test
    void batchSettingSeasonsReplacesSelectedClothingSeasons() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 218L);
        Clothing first = saveClothing(Clothing.ready(owner, category, "First", "blue", "first.png", "first.png", "image/png", 8L), 133L);
        Clothing second = saveClothing(Clothing.ready(owner, category, "Second", "navy", "second.png", "second.png", "image/png", 8L), 134L);
        seasons.add(new ClothingSeason(first, com.fan.mixmyfit.domain.Season.SPRING));
        seasons.add(new ClothingSeason(second, com.fan.mixmyfit.domain.Season.AUTUMN));

        mvc.perform(post("/api/clothes/batch")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clothingIds": [133, 134],
                                  "seasons": ["summer", "winter"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(seasonValuesFor(first)).containsExactly("summer", "winter");
        assertThat(seasonValuesFor(second)).containsExactly("summer", "winter");
    }

    @Test
    void batchAddingAndRemovingTagsOnlyAffectsSelectedCurrentUserClothing() throws Exception {
        Category category = saveCategory(Category.custom(owner, "Layer"), 219L);
        ClothingTag office = saveTag(new ClothingTag(owner, "Office"), 320L);
        ClothingTag casual = saveTag(new ClothingTag(owner, "Casual"), 321L);
        ClothingTag otherTag = saveTag(new ClothingTag(anotherUser, "Office"), 322L);
        Clothing first = saveClothing(Clothing.ready(owner, category, "First", "blue", "first.png", "first.png", "image/png", 8L), 135L);
        Clothing second = saveClothing(Clothing.ready(owner, category, "Second", "navy", "second.png", "second.png", "image/png", 8L), 136L);
        Clothing unselected = saveClothing(Clothing.ready(owner, category, "Third", "gray", "third.png", "third.png", "image/png", 8L), 137L);
        Clothing otherUsersClothing = saveClothing(Clothing.ready(anotherUser, category, "Other", "red", "other.png", "other.png", "image/png", 8L), 138L);
        tagLinks.add(new ClothingTagLink(first, casual));
        tagLinks.add(new ClothingTagLink(second, casual));
        tagLinks.add(new ClothingTagLink(unselected, casual));
        tagLinks.add(new ClothingTagLink(otherUsersClothing, otherTag));

        mvc.perform(post("/api/clothes/batch")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clothingIds": [135, 136],
                                  "addTagIds": [320],
                                  "removeTagIds": [321]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updated").value(2));

        assertThat(tagIdsFor(first)).containsExactly(320L);
        assertThat(tagIdsFor(second)).containsExactly(320L);
        assertThat(tagIdsFor(unselected)).containsExactly(321L);
        assertThat(tagIdsFor(otherUsersClothing)).containsExactly(322L);
    }

    @Test
    void batchRejectsMixedUserClothingIdsWithoutMutatingAnyData() throws Exception {
        Clothing ownerClothing = saveClothing(Clothing.draft(owner, "owner.png", "owner.png", "image/png", 8L), 139L);
        Clothing otherClothing = saveClothing(Clothing.draft(anotherUser, "other.png", "other.png", "image/png", 8L), 140L);
        Category category = saveCategory(Category.custom(owner, "Layer"), 220L);

        mvc.perform(post("/api/clothes/batch")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "clothingIds": [139, 140],
                                  "categoryId": 220,
                                  "color": "blue",
                                  "seasons": ["spring"]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        assertThat(ownerClothing.getCategory()).isNull();
        assertThat(ownerClothing.getStatus().dbValue()).isEqualTo("draft");
        assertThat(ownerClothing.getColor()).isNull();
        assertThat(otherClothing.getCategory()).isNull();
        assertThat(otherClothing.getStatus().dbValue()).isEqualTo("draft");
        assertThat(seasons).isEmpty();
        assertThat(category.getName()).isEqualTo("Layer");
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

    private List<String> seasonValuesFor(Clothing clothing) {
        return seasons.stream()
                .filter(season -> season.getClothing().equals(clothing))
                .map(season -> season.getSeason().dbValue())
                .toList();
    }

    private List<Long> tagIdsFor(Clothing clothing) {
        return tagLinks.stream()
                .filter(link -> link.getClothing().equals(clothing))
                .map(link -> link.getClothingTag().getClothingTagId())
                .toList();
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
        setField(target, fieldName, id);
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
