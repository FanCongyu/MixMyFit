package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Long> {
}
