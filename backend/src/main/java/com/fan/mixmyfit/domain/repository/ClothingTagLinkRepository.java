package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.ClothingTagLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingTagLinkRepository extends JpaRepository<ClothingTagLink, Long> {
    List<ClothingTagLink> findByClothingClothingIdOrderByClothingTagLinkId(Long clothingId);

    void deleteByClothingClothingId(Long clothingId);
}
