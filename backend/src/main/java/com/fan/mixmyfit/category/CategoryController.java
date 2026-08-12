package com.fan.mixmyfit.category;

import com.fan.mixmyfit.security.SessionCookieFactory;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
class CategoryController {
    private final CategoryService categories;

    CategoryController(CategoryService categories) {
        this.categories = categories;
    }

    @GetMapping
    List<CategoryResponse> list(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId) {
        return categories.list(sessionId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoryResponse create(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @RequestBody CategoryRequest request) {
        return categories.create(sessionId, request);
    }

    @GetMapping("/{categoryId}")
    CategoryResponse get(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long categoryId) {
        return categories.get(sessionId, categoryId);
    }

    @PatchMapping("/{categoryId}")
    CategoryResponse update(
            @CookieValue(name = SessionCookieFactory.SESSION_COOKIE_NAME, required = false) String sessionId,
            @PathVariable Long categoryId,
            @RequestBody CategoryRequest request) {
        return categories.update(sessionId, categoryId, request);
    }
}
