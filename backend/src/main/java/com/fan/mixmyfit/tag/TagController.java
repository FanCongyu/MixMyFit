package com.fan.mixmyfit.tag;

import com.fan.mixmyfit.security.SessionCookieFactory;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TagController {
    private final TagService tags;

    TagController(TagService tags) {
        this.tags = tags;
    }

    @GetMapping("/api/clothing-tags")
    List<TagResponse> listClothingTags(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return tags.listClothingTags(sessionId);
    }

    @PostMapping("/api/clothing-tags")
    @ResponseStatus(HttpStatus.CREATED)
    TagResponse createClothingTag(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody TagRequest request) {
        return tags.createClothingTag(sessionId, request);
    }

    @GetMapping("/api/outfit-tags")
    List<TagResponse> listOutfitTags(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return tags.listOutfitTags(sessionId);
    }

    @PostMapping("/api/outfit-tags")
    @ResponseStatus(HttpStatus.CREATED)
    TagResponse createOutfitTag(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody TagRequest request) {
        return tags.createOutfitTag(sessionId, request);
    }
}
