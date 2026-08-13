package com.fan.mixmyfit.outfit;

import com.fan.mixmyfit.security.SessionCookieFactory;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/outfits")
public class OutfitController {
    private final OutfitService outfits;

    public OutfitController(OutfitService outfits) {
        this.outfits = outfits;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    OutfitCreateResponse create(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody OutfitCreateRequest request) {
        return outfits.create(sessionId, request);
    }

    @GetMapping("/{outfitId}")
    OutfitDetailResponse detail(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long outfitId) {
        return outfits.detail(sessionId, outfitId);
    }

    @GetMapping
    OutfitListResponse list(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String season,
            @RequestParam(required = false) List<Long> tagIds) {
        return outfits.list(sessionId, page, size, season, tagIds);
    }

    @PatchMapping("/{outfitId}")
    OutfitDetailResponse update(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long outfitId,
            @RequestBody OutfitCreateRequest request) {
        return outfits.update(sessionId, outfitId, request);
    }

    @DeleteMapping("/{outfitId}")
    ResponseEntity<Void> delete(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long outfitId) {
        outfits.delete(sessionId, outfitId);
        return ResponseEntity.noContent().build();
    }
}
