package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Outfit;
import com.fan.mixmyfit.domain.OutfitSeason;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutfitSeasonRepository extends JpaRepository<OutfitSeason, Long> {
    List<OutfitSeason> findByOutfit(Outfit outfit);

    void deleteByOutfit(Outfit outfit);
}
