package com.fan.mixmyfit.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fan.mixmyfit.clothing.ClothingController;
import com.fan.mixmyfit.clothing.ClothingService;
import com.fan.mixmyfit.domain.Clothing;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.CategoryRepository;
import com.fan.mixmyfit.domain.repository.ClothingRepository;
import com.fan.mixmyfit.domain.repository.ClothingSeasonRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagLinkRepository;
import com.fan.mixmyfit.domain.repository.ClothingTagRepository;
import com.fan.mixmyfit.security.CurrentUserResolver;
import com.fan.mixmyfit.security.SecurityExceptionHandler;
import com.fan.mixmyfit.security.SessionRegistry;
import com.fan.mixmyfit.security.SessionCookieFactory;
import com.fan.mixmyfit.domain.repository.UserRepository;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SecureUploadEndpointTest {
    private static final String SESSION_ID = "session-1";

    @TempDir
    Path uploadDir;

    private ClothingRepository clothes;
    private List<Clothing> savedClothes;
    private MockMvc mvc;
    private User owner;
    private Long nextClothingId;

    @BeforeEach
    void setUp() {
        owner = user(1L, "owner");
        nextClothingId = 100L;
        savedClothes = new ArrayList<>();
        clothes = fakeClothingRepository();
        CurrentUserResolver currentUsers = fakeCurrentUsers();

        StoredFileService storedFiles = new StoredFileService(uploadDir.toString());
        ClothingService service = new ClothingService(
                currentUsers,
                repositoryProvider(clothes),
                storedFiles,
                repositoryProvider(null),
                repositoryProvider(null),
                repositoryProvider(null),
                repositoryProvider(null));

        mvc = MockMvcBuilders.standaloneSetup(new ClothingController(service))
                .setControllerAdvice(new FileExceptionHandler(), new SecurityExceptionHandler())
                .build();
    }

    @Test
    void uploadingImageFileFieldCreatesDraftClothing() throws Exception {
        mvc.perform(multipart("/api/clothes")
                        .file(file("file", "shirt.png", MediaType.IMAGE_PNG_VALUE, tinyPng()))
                        .cookie(sessionCookie()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.clothingId").value(100))
                .andExpect(jsonPath("$.status").value("draft"))
                .andExpect(jsonPath("$.imageUrl").value("/api/clothes/100/image"))
                .andExpect(jsonPath("$.originalFilename").value("shirt.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.fileSize").value(tinyPng().length));
    }

    @Test
    void nonImageMimeTypeIsRejected() throws Exception {
        mvc.perform(multipart("/api/clothes")
                        .file(file("file", "notes.txt", MediaType.TEXT_PLAIN_VALUE, "not image".getBytes()))
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_CONTENT_TYPE_NOT_ALLOWED"));
    }

    @Test
    void filesOverFiveMegabytesAreRejected() throws Exception {
        byte[] oversized = new byte[(5 * 1024 * 1024) + 1];

        mvc.perform(multipart("/api/clothes")
                        .file(file("file", "large.png", MediaType.IMAGE_PNG_VALUE, oversized))
                        .cookie(sessionCookie()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UPLOAD_FILE_TOO_LARGE"));
    }

    @Test
    void storedFilenameDoesNotTrustOriginalFilename() throws Exception {
        mvc.perform(multipart("/api/clothes")
                        .file(file("file", "../unsafe.png", MediaType.IMAGE_PNG_VALUE, tinyPng()))
                        .cookie(sessionCookie()))
                .andExpect(status().isCreated());

        Clothing saved = savedClothing();
        String storedFilename = Path.of(saved.getImagePath()).getFileName().toString();
        assertThat(saved.getOriginalFilename()).isEqualTo("../unsafe.png");
        assertThat(storedFilename).isNotEqualTo("../unsafe.png");
        assertThat(storedFilename).isNotEqualTo("unsafe.png");
        assertThat(Files.exists(Path.of(saved.getImagePath()))).isTrue();
    }

    @Test
    void userCannotReadAnotherUsersUploadedImage() throws Exception {
        User anotherUser = user(2L, "another");
        Path stored = uploadDir.resolve("private.webp");
        Files.writeString(stored, "webp", StandardCharsets.UTF_8);
        Clothing clothing = withId(Clothing.draft(
                anotherUser,
                stored.toString(),
                "private.webp",
                "image/webp",
                4L), 200L);
        savedClothes.add(clothing);

        mvc.perform(get("/api/clothes/200/image").cookie(sessionCookie()))
                .andExpect(status().isNotFound());
    }

    @Test
    void ownerCanReadUploadedImage() throws Exception {
        Path stored = uploadDir.resolve("owner.png");
        Files.write(stored, tinyPng());
        Clothing clothing = withId(Clothing.draft(
                owner,
                stored.toString(),
                "owner.png",
                "image/png",
                tinyPng().length), 201L);
        savedClothes.add(clothing);

        mvc.perform(get("/api/clothes/201/image").cookie(sessionCookie()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "image/png"))
                .andExpect(content().bytes(tinyPng()));
    }

    private Clothing savedClothing() {
        return savedClothes.get(savedClothes.size() - 1);
    }

    private CurrentUserResolver fakeCurrentUsers() {
        return new CurrentUserResolver((SessionRegistry) null, (UserRepository) null) {
            @Override
            public User requireUser(String sessionId) {
                return owner;
            }

            @Override
            public Long requireUserId(String sessionId) {
                return owner.getUserId();
            }
        };
    }

    private ClothingRepository fakeClothingRepository() {
        InvocationHandler handler = (proxy, method, args) -> switch (method.getName()) {
            case "save" -> {
                Clothing clothing = withId((Clothing) args[0], nextClothingId++);
                savedClothes.add(clothing);
                yield clothing;
            }
            case "findById" -> savedClothes.stream()
                    .filter(clothing -> clothing.getClothingId().equals(args[0]))
                    .findFirst();
            case "toString" -> "FakeClothingRepository";
            default -> throw new UnsupportedOperationException(method.getName());
        };
        return (ClothingRepository) Proxy.newProxyInstance(
                ClothingRepository.class.getClassLoader(),
                new Class<?>[] { ClothingRepository.class },
                handler);
    }

    private <T> ObjectProvider<T> repositoryProvider(T repository) {
        return new ObjectProvider<>() {
            @Override
            public T getObject(Object... args) {
                return repository;
            }

            @Override
            public T getIfAvailable() {
                return repository;
            }

            @Override
            public T getIfUnique() {
                return repository;
            }

            @Override
            public T getObject() {
                return repository;
            }
        };
    }

    private static MockMultipartFile file(String field, String filename, String contentType, byte[] bytes) {
        return new MockMultipartFile(field, filename, contentType, bytes);
    }

    private static jakarta.servlet.http.Cookie sessionCookie() {
        return new jakarta.servlet.http.Cookie(SessionCookieFactory.SESSION_COOKIE_NAME, SESSION_ID);
    }

    private static byte[] tinyPng() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
    }

    private static User user(Long userId, String username) {
        User user = new User(username, "hash", username);
        setId(user, "userId", userId);
        return user;
    }

    private static Clothing withId(Clothing clothing, Long clothingId) {
        setId(clothing, "clothingId", clothingId);
        return clothing;
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
