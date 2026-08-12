package com.fan.mixmyfit.category;

import com.fan.mixmyfit.domain.Category;

record CategoryResponse(Long categoryId, String name, String type) {
    static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getCategoryId(),
                category.getName(),
                category.getType().dbValue());
    }
}
