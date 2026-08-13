package com.fan.mixmyfit.outfit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class OutfitSaveEndpointTest {
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
    void emptyOutfitCannotBeSaved() throws Exception {
        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Empty",
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OUTFIT"));

        assertThat(outfits).isEmpty();
    }

    @Test
    void partialMainSlotsCanBeSaved() throws Exception {
        saveClothing(Clothing.ready(owner, ownerCategory, "Blue Shirt", "blue", "top.png", "top.png", "image/png", 8L), 100L);
        saveClothing(Clothing.ready(owner, ownerCategory, "Loafers", "black", "shoes.png", "shoes.png", "image/png", 8L), 101L);

        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Work outfit",
                                  "items": [
                                    { "clothingId": 100, "role": "main_slot", "slot": "top" },
                                    { "clothingId": 101, "role": "main_slot", "slot": "shoes" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outfitId").value(1000))
                .andExpect(jsonPath("$.title").value("Work outfit"));

        assertThat(items).hasSize(2);
        assertThat(items).extracting(item -> item.getSlot().dbValue()).containsExactly("top", "shoes");
    }

    @Test
    void blankTitleGeneratesDefaultTitle() throws Exception {
        saveClothing(Clothing.ready(owner, ownerCategory, "Blue Shirt", "blue", "top.png", "top.png", "image/png", 8L), 102L);

        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "   ",
                                  "items": [
                                    { "clothingId": 102, "role": "main_slot", "slot": "top" }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Untitled Outfit"));
    }

    @Test
    void validPayloadSavesMetadataAndAccessoryPlacement() throws Exception {
        saveClothing(Clothing.ready(owner, ownerCategory, "Hat", "black", "hat.png", "hat.png", "image/png", 8L), 103L);
        saveClothing(Clothing.ready(owner, ownerCategory, "Scarf", "red", "scarf.png", "scarf.png", "image/png", 8L), 104L);
        saveTag(new OutfitTag(owner, "Office"), 300L);

        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Layered",
                                  "note": "Rain day",
                                  "seasons": ["spring", "winter"],
                                  "tagIds": [300],
                                  "items": [
                                    { "clothingId": 103, "role": "main_slot", "slot": "hat" },
                                    {
                                      "clothingId": 104,
                                      "role": "accessory_overlay",
                                      "positionX": 12,
                                      "positionY": 34,
                                      "size": "large",
                                      "zIndex": 7
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.outfitId").value(1000))
                .andExpect(jsonPath("$.title").value("Layered"));

        assertThat(outfits).hasSize(1);
        assertThat(outfits.get(0).getNote()).isEqualTo("Rain day");
        assertThat(seasons).extracting(season -> season.getSeason().dbValue()).containsExactly("spring", "winter");
        assertThat(tagLinks).hasSize(1);
        OutfitItem accessory = items.get(1);
        assertThat(accessory.getPositionX()).isEqualTo(12);
        assertThat(accessory.getPositionY()).isEqualTo(34);
        assertThat(accessory.getSize()).isEqualTo(OutfitItemSize.LARGE);
        assertThat(accessory.getZIndex()).isEqualTo(7);
    }

    @Test
    void draftClothingCannotBeSavedIntoOutfit() throws Exception {
        saveClothing(Clothing.draft(owner, "draft.png", "draft.png", "image/png", 8L), 105L);

        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "clothingId": 105, "role": "main_slot", "slot": "top" }
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_OUTFIT"));

        assertThat(outfits).isEmpty();
    }

    @Test
    void cannotSaveOutfitWithAnotherUsersClothing() throws Exception {
        saveClothing(Clothing.ready(anotherUser, otherCategory, "Private", "black", "private.png", "private.png", "image/png", 8L), 106L);

        mvc.perform(post("/api/outfits")
                        .cookie(sessionCookie())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    { "clothingId": 106, "role": "main_slot", "slot": "top" }
                                  ]
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));

        assertThat(outfits).isEmpty();
    }

    private Clothing saveClothing(Clothing clothing, Long clothingId) {
        setId(clothing, "clothingId", clothingId);
        clothes.add(clothing);
        return clothing;
    }

    private OutfitTag saveTag(OutfitTag tag, Long tagId) {
        setId(tag, "outfitTagId", tagId);
        tags.add(tag);
        return tag;
    }

    private OutfitRepository outfitRepository() {
        return (OutfitRepository) java.lang.reflect.Proxy.newProxyInstance(
                OutfitRepository.class.getClassLoader(),
                new Class<?>[] { OutfitRepository.class },
                (proxy, method, args) -> switch (method.getName()) {
                    case "save" -> {
                        Outfit outfit = (Outfit) args[0];
                        setId(outfit, "outfitId", 1000L + outfits.size());
                        outfits.add(outfit);
                        yield outfit;
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
