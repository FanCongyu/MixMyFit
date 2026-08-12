package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.ClothingTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingTagRepository extends JpaRepository<ClothingTag, Long> {
    List<ClothingTag> findByUserUserIdOrderByClothingTagId(Long userId);

    boolean existsByUserUserIdAndName(Long userId, String name);
}
