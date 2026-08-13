package com.fan.mixmyfit.outfit;

import com.fan.mixmyfit.security.SessionCookieFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
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
}
