package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Outfit;
import com.fan.mixmyfit.domain.OutfitItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitItemRepository extends JpaRepository<OutfitItem, Long> {
    List<OutfitItem> findByOutfit(Outfit outfit);

    void deleteByOutfit(Outfit outfit);
}
