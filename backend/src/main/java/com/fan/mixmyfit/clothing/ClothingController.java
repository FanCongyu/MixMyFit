package com.fan.mixmyfit.clothing;

import com.fan.mixmyfit.file.ClothingImage;
import com.fan.mixmyfit.security.SessionCookieFactory;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/clothes")
public class ClothingController {
    private final ClothingService clothes;

    public ClothingController(ClothingService clothes) {
        this.clothes = clothes;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClothingUploadResponse upload(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestParam("file") MultipartFile file) {
        return clothes.upload(sessionId, file);
    }

    @GetMapping
    public ClothingListResponse list(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String color,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) List<Long> tagIds) {
        return clothes.list(sessionId, page, size, categoryId, status, color, season, tagIds);
    }

    @GetMapping("/colors")
    public List<String> colors(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return clothes.colors(sessionId);
    }

    @GetMapping("/draft-count")
    public DraftCountResponse draftCount(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return clothes.draftCount(sessionId);
    }

    @GetMapping("/{clothingId}")
    public ClothingResponse get(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long clothingId) {
        return clothes.get(sessionId, clothingId);
    }

    @PostMapping("/batch")
    public ClothingBatchResponse batch(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody ClothingBatchRequest request) {
        return clothes.batch(sessionId, request);
    }

    @PatchMapping("/{clothingId}")
    public ClothingResponse update(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long clothingId,
            @RequestBody ClothingUpdateRequest request) {
        return clothes.update(sessionId, clothingId, request);
    }

    @DeleteMapping("/{clothingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long clothingId) {
        clothes.delete(sessionId, clothingId);
    }

    @GetMapping("/{clothingId}/image")
    public ResponseEntity<byte[]> image(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long clothingId) {
        ClothingImage image = clothes.image(sessionId, clothingId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, image.contentType())
                .contentType(MediaType.parseMediaType(image.contentType()))
                .body(image.content());
    }
}
