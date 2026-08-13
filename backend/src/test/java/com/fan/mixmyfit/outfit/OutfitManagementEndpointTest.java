package com.fan.mixmyfit.outfit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.Outfit;
import com.fan.mixmyfit.domain.OutfitItem;
import com.fan.mixmyfit.domain.OutfitItemSize;
import com.fan.mixmyfit.domain.OutfitSeason;
import com.fan.mixmyfit.domain.OutfitTag;
import com.fan.mixmyfit.domain.OutfitTagLink;
import com.fan.mixmyfit.domain.Season;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.OutfitItemRepository;
import com.fan.mixmyfit.domain.repository.OutfitRepository;
import com.fan.mixmyfit.domain.repository.OutfitSeasonRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagLinkRepository;
import com.fan.mixmyfit.domain.repository.OutfitTagRepository;
import com.fan.mixmyfit.domain.repository.UserRepository;
import com.fan.mixmyfit.security.CurrentUserResolver;
import com.fan.mixmyfit.security.SecurityExceptionHandler;
import com.fan.mixmyfit.security.SessionCookieFactory;
import com.fan.mixmyfit.security.SessionRegistry;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OutfitManagementEndpointTest {
    private static final String SESSION_ID = "session-1";

    private final User owner = user(1L, "owner");
    private final User anotherUser = user(2L, "another");
    private final Category ownerCategory = category(owner, 10L);
    private final Category otherCategory = category(anotherUser, 11L);
    private final List<Clothing> clothes = new ArrayList<>();
    private final List<Outfit> outfits = new ArrayList<>();
    private final List<OutfitItem> items = new ArrayList<>();
    private final List<OutfitSeason> seasons = new ArrayList<>();
    private final List<OutfitTag> tags = new ArrayList<>();
    private final List<OutfitTagLink> tagLinks = new ArrayList<>();

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

        OutfitService service = new OutfitService(
                currentUsers,
                provider(outfitRepository()),
                provider(clothingRepository()),
                provider(outfitItemRepository()),
                provider(outfitSeasonRepository()),
                provider(outfitTagRepository()),
                provider(outfitTagLinkRepository()));

        mvc = MockMvcBuilders.standaloneSetup(new OutfitController(service))
                .setControllerAdvice(new OutfitExceptionHandler(), new SecurityExceptionHandler())
                .build();
    }

    @Test
    void userCanFilterOwnOutfitsBySeason() throws Exception {
        Outfit spring = saveOutfit(owner, 1000L, "Spring office");
        Outfit winter = saveOutfit(owner, 1001L, "Winter weekend");
        Outfit other = saveOutfit(anotherUser, 1002L, "Other spring");
        saveSeason(spring, Season.SPRING);
        saveSeason(winter, Season.WINTER);
        saveSeason(other, Season.SPRING);

        mvc.perform(get("/api/outfits")
                        .cookie(sessionCookie())
                        .param("season", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].outfitId").value(1000))
                .andExpect(jsonPath("$.items[0].title").value("Spring office"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void userCanFilterOwnOutfitsByOutfitTag() throws Exception {
        Outfit office = saveOutfit(owner, 1000L, "Office look");
        Outfit weekend = saveOutfit(owner, 1001L, "Weekend look");
        OutfitTag officeTag = saveTag(owner, 300L, "Office");
        OutfitTag weekendTag = saveTag(owner, 301L, "Weekend");
        linkTag(office, officeTag);
        linkTag(weekend, weekendTag);

        mvc.perform(get("/api/outfits")
                        .cookie(sessionCookie())
                        .param("tagIds", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].outfitId").value(1000))
                .andExpect(jsonPath("$.items[0].title").value("Office look"));
    }

    @Test
    void seasonAndTagFiltersAreCombined() throws Exception {
        Outfit springOffice = saveOutfit(owner, 1000L, "Spring office");
        Outfit winterOffice = saveOutfit(owner, 1001L, "Winter office");
        Outfit springWeekend = saveOutfit(owner, 1002L, "Spring weekend");
        OutfitTag officeTag = saveTag(owner, 300L, "Office");
        OutfitTag weekendTag = saveTag(owner, 301L, "Weekend");
        saveSeason(springOffice, Season.SPRING);
        saveSeason(winterOffice, Season.WINTER);
        saveSeason(springWeekend, Season.SPRING);
        linkTag(springOffice, officeTag);
        linkTag(winterOffice, officeTag);
        linkTag(springWeekend, weekendTag);

        mvc.perform(get("/api/outfits")
                        .cookie(sessionCookie())
                        .param("season", "spring")
                        .param("tagIds", "300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].outfitId").value(1000));
    }

    @Test
    void detailIncludesItemsSeasonsAndTags() throws Exception {
        Clothing top = saveClothing(Clothing.ready(owner, ownerCategory, "Shirt", "blue", "shirt.png", "shirt.png", "image/png", 8L), 100L);
        Clothing scarf = saveClothing(Clothing.ready(owner, ownerCategory, "Scarf", "red", "scarf.png", "scarf.png", "image/png", 8L), 101L);
        Outfit outfit = saveOutfit(owner, 1000L, "Layered");
        saveSeason(outfit, Season.SPRING);
        OutfitTag office = saveTag(owner, 300L, "Office");
        linkTag(outfit, office);
        saveItem(OutfitItem.mainSlot(outfit, owner, top.getClothingId(), com.fan.mixmyfit.domain.OutfitSlot.TOP));
        saveItem(OutfitItem.accessory(outfit, owner, scarf.getClothingId(), 12, 34, OutfitItemSize.LARGE, 7));

        mvc.perform(get("/api/outfits/1000")
                        .cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outfitId").value(1000))
                .andExpect(jsonPath("$.title").value("Layered"))
                .andExpect(jsonPath("$.seasons[0]").value("spring"))
                .andExpect(jsonPath("$.tags[0].tagId").value(300))
                .andExpect(jsonPath("$.tags[0].name").value("Office"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].clothingId").value(100))
                .andExpect(jsonPath("$.items[0].role").value("main_slot"))
                .andExpect(jsonPath("$.items[0].slot").value("top"))
                .andExpect(jsonPath("$.items[1].clothingId").value(101))
                .andExpect(jsonPath("$.items[1].role").value("accessory_overlay"))
                .andExpect(jsonPath("$.items[1].positionX").value(12))
                .andExpect(jsonPath("$.items[1].positionY").value(34))
                .andExpect(jsonPath("$.items[1].size").value("large"))
                .andExpect(jsonPath("$.items[1].zIndex").value(7));
    }

    @Test
    void patchUpdatesOutfitContentAndMetadata() throws Exception {
        Clothing top = saveClothing(Clothing.ready(owner, ownerCategory, "Shirt", "blue", "shirt.png", "shirt.png", "image/png", 8L), 100L);
        Clothing shoes = saveClothing(Clothing.ready(owner, ownerCategory, "Shoes", "black", "shoes.png", "shoes.png", "image/png", 8L), 101L);
        Outfit outfit = saveOutfit(owner, 1000L, "Old title");
        saveItem(OutfitItem.mainSlot(outfit, owner, top.getClothingId(), com.fan.mixmyfit.domain.OutfitSlot.TOP));
        OutfitTag office = saveTag(owner, 300L, "Office");
        OutfitTag travel = saveTag(owner, 301L, "Travel");
        linkTag(outfit, office);
        saveSeason(outfit, Season.SPRING);

        mvc.perform(patch("/api/outfits/1000")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated",
                                  "note": "Changed note",
                                  "seasons": ["winter"],
                                  "tagIds": [301],
                                  "items": [
                                    { "clothingId": 101, "role": "main_slot", "slot": "shoes" }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.note").value("Changed note"))
                .andExpect(jsonPath("$.seasons[0]").value("winter"))
                .andExpect(jsonPath("$.tags[0].tagId").value(301))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].clothingId").value(101))
                .andExpect(jsonPath("$.items[0].slot").value("shoes"));

        assertThat(outfit.getTitle()).isEqualTo("Updated");
        assertThat(items).hasSize(1);
        assertThat(seasons).extracting(season -> season.getSeason()).containsExactly(Season.WINTER);
        assertThat(tagLinks).extracting(link -> link.getOutfitTag().getOutfitTagId()).containsExactly(301L);
    }

    @Test
    void userCannotViewEditOrDeleteAnotherUsersOutfit() throws Exception {
        saveOutfit(anotherUser, 1000L, "Private");

        mvc.perform(get("/api/outfits/1000").cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(patch("/api/outfits/1000")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                { "title": "Hacked", "items": [] }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        mvc.perform(delete("/api/outfits/1000").cookie(sessionCookie()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void userCanDeleteOwnOutfit() throws Exception {
        Outfit outfit = saveOutfit(owner, 1000L, "Delete me");
        saveItem(OutfitItem.mainSlot(outfit, owner, 100L, com.fan.mixmyfit.domain.OutfitSlot.TOP));
        saveSeason(outfit, Season.SUMMER);

        mvc.perform(delete("/api/outfits/1000").cookie(sessionCookie()))
                .andExpect(status().isNoContent());

        assertThat(outfits).isEmpty();
        assertThat(items).isEmpty();
        assertThat(seasons).isEmpty();
    }

    private Outfit saveOutfit(User user, Long outfitId, String title) {
        Outfit outfit = new Outfit(user, title, null);
        setId(outfit, "outfitId", outfitId);
        outfits.add(outfit);
        return outfit;
    }

    private Clothing saveClothing(Clothing clothing, Long clothingId) {
        setId(clothing, "clothingId", clothingId);
        clothes.add(clothing);
        return clothing;
    }

    private OutfitTag saveTag(User user, Long tagId, String name) {
        OutfitTag tag = new OutfitTag(user, name);
        setId(tag, "outfitTagId", tagId);
        tags.add(tag);
        return tag;
    }

    private void saveSeason(Outfit outfit, Season season) {
        seasons.add(new OutfitSeason(outfit, season));
    }

    private void linkTag(Outfit outfit, OutfitTag tag) {
        tagLinks.add(new OutfitTagLink(outfit, tag));
    }

    private void saveItem(OutfitItem item) {
        items.add(item);
    }

    private OutfitRepository outfitRepository() {
        return (OutfitRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitRepository.class.getClassLoader(),
                new Class<?>[] { OutfitRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> args[0];
                    case "findById" -> outfits.stream()
                            .filter(outfit -> outfit.getOutfitId().equals(args[0]))
                            .findFirst();
                    case "findAll" -> outfits;
                    case "delete" -> {
                        outfits.remove(args[0]);
                        yield null;
                    }
                    case "toString" -> "FakeOutfitRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private ClothingRepository clothingRepository() {
        return (ClothingRepository) java.lang.reflect.Proxy.newProxyInstance(
                ClothingRepository.class.getClassLoader(),
                new Class<?>[] { ClothingRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> clothes.stream()
                            .filter(clothing -> clothing.getClothingId().equals(args[0]))
                            .findFirst();
                    case "toString" -> "FakeClothingRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private OutfitItemRepository outfitItemRepository() {
        return (OutfitItemRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitItemRepository.class.getClassLoader(),
                new Class<?>[] { OutfitItemRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        for (Object item : (Iterable<?>) args[0]) {
                            items.add((OutfitItem) item);
                        }
                        yield args[0];
                    }
                    case "findByOutfit" -> items.stream()
                            .filter(item -> item.getOutfit().equals(args[0]))
                            .toList();
                    case "deleteByOutfit" -> {
                        items.removeIf(item -> item.getOutfit().equals(args[0]));
                        yield null;
                    }
                    case "toString" -> "FakeOutfitItemRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private OutfitSeasonRepository outfitSeasonRepository() {
        return (OutfitSeasonRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitSeasonRepository.class.getClassLoader(),
                new Class<?>[] { OutfitSeasonRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        for (Object season : (Iterable<?>) args[0]) {
                            seasons.add((OutfitSeason) season);
                        }
                        yield args[0];
                    }
                    case "findByOutfit" -> seasons.stream()
                            .filter(season -> season.getOutfit().equals(args[0]))
                            .toList();
                    case "deleteByOutfit" -> {
                        seasons.removeIf(season -> season.getOutfit().equals(args[0]));
                        yield null;
                    }
                    case "toString" -> "FakeOutfitSeasonRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private OutfitTagRepository outfitTagRepository() {
        return (OutfitTagRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitTagRepository.class.getClassLoader(),
                new Class<?>[] { OutfitTagRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "findById" -> tags.stream()
                            .filter(tag -> tag.getOutfitTagId().equals(args[0]))
                            .findFirst();
                    case "toString" -> "FakeOutfitTagRepository";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
    }

    private OutfitTagLinkRepository outfitTagLinkRepository() {
        return (OutfitTagLinkRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitTagLinkRepository.class.getClassLoader(),
                new Class<?>[] { OutfitTagLinkRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "saveAll" -> {
                        for (Object link : (Iterable<?>) args[0]) {
                            tagLinks.add((OutfitTagLink) link);
                        }
                        yield args[0];
                    }
                    case "findByOutfit" -> tagLinks.stream()
                            .filter(link -> link.getOutfit().equals(args[0]))
                            .toList();
                    case "deleteByOutfit" -> {
                        tagLinks.removeIf(link -> link.getOutfit().equals(args[0]));
                        yield null;
                    }
                    case "toString" -> "FakeOutfitTagLinkRepository";
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

    private static Category category(User user, Long categoryId) {
        Category category = Category.custom(user, "Layer");
        setId(category, "categoryId", categoryId);
        return category;
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
