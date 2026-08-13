package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Outfit;
import com.fan.mixmyfit.domain.OutfitTagLink;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitTagLinkRepository extends JpaRepository<OutfitTagLink, Long> {
    List<OutfitTagLink> findByOutfit(Outfit outfit);

    void deleteByOutfit(Outfit outfit);
}
