package com.fan.mixmyfit.category;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.CategoryType;
import com.fan.mixmyfit.domain.User;
import com.fan.mixmyfit.domain.repository.CategoryRepository;
import com.fan.mixmyfit.security.CurrentUserResolver;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class CategoryService {
    private static final List<String> FIXED_CATEGORY_NAMES = List.of("上装", "下装", "鞋子", "帽子");

    private final ObjectProvider<CategoryRepository> categoryRepositories;
    private final CurrentUserResolver currentUsers;

    CategoryService(ObjectProvider<CategoryRepository> categoryRepositories, CurrentUserResolver currentUsers) {
        this.categoryRepositories = categoryRepositories;
        this.currentUsers = currentUsers;
    }

    @Transactional
    List<CategoryResponse> list(String sessionId) {
        User user = currentUsers.requireUser(sessionId);
        ensureFixedCategories();

        List<Category> visible = new ArrayList<>();
        visible.addAll(categories().findByUserIsNullOrderByCategoryId());
        visible.addAll(categories().findByUserUserIdOrderByCategoryId(user.getUserId()));
        return visible.stream().map(CategoryResponse::from).toList();
    }

    @Transactional
    CategoryResponse create(String sessionId, CategoryRequest request) {
        User user = currentUsers.requireUser(sessionId);
        String name = requiredName(request);
        if (categories().existsByUserUserIdAndName(user.getUserId(), name)) {
            throw new CategoryException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }
        try {
            return CategoryResponse.from(categories().saveAndFlush(Category.custom(user, name)));
        } catch (DataIntegrityViolationException exception) {
            throw new CategoryException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }
    }

    @Transactional(readOnly = true)
    CategoryResponse get(String sessionId, Long categoryId) {
        Long userId = currentUsers.requireUserId(sessionId);
        Category category = categories().findById(categoryId)
                .filter(candidate -> candidate.getUser() == null
                        || candidate.getUser().getUserId().equals(userId))
                .orElseThrow(() -> new CategoryException("CATEGORY_NOT_FOUND", "Category not found"));
        return CategoryResponse.from(category);
    }

    @Transactional
    CategoryResponse update(String sessionId, Long categoryId, CategoryRequest request) {
        Long userId = currentUsers.requireUserId(sessionId);
        String name = requiredName(request);
        Category category = categories().findByCategoryIdAndUserUserId(categoryId, userId)
                .orElseThrow(() -> new CategoryException("CATEGORY_NOT_FOUND", "Category not found"));
        if (categories().existsByUserUserIdAndName(userId, name)) {
            throw new CategoryException("CATEGORY_NAME_EXISTS", "Category name already exists");
        }
        category.rename(name);
        return CategoryResponse.from(category);
    }

    private void ensureFixedCategories() {
        for (String name : FIXED_CATEGORY_NAMES) {
            if (!categories().existsByUserIsNullAndNameAndType(name, CategoryType.FIXED)) {
                categories().save(Category.fixed(name));
            }
        }
    }

    private CategoryRepository categories() {
        return categoryRepositories.getObject();
    }

    private static String requiredName(CategoryRequest request) {
        if (request == null || request.name() == null || request.name().isBlank()) {
            throw new CategoryException("CATEGORY_NAME_REQUIRED", "Category name is required");
        }
        return request.name().trim();
    }
}
