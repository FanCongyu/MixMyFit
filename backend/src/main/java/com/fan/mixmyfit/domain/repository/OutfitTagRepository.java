package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.OutfitTag;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitTagRepository extends JpaRepository<OutfitTag, Long> {
    List<OutfitTag> findByUserUserIdOrderByOutfitTagId(Long userId);

    boolean existsByUserUserIdAndName(Long userId, String name);
}
