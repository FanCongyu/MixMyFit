package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Category;
import com.fan.mixmyfit.domain.CategoryType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    List<Category> findByUserIsNullOrderByCategoryId();

    List<Category> findByUserUserIdOrderByCategoryId(Long userId);

    Optional<Category> findByCategoryIdAndUserUserId(Long categoryId, Long userId);

    boolean existsByUserIsNullAndNameAndType(String name, CategoryType type);

    boolean existsByUserUserIdAndName(Long userId, String name);
}
