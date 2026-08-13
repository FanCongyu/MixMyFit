package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.ClothingSeason;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingSeasonRepository extends JpaRepository<ClothingSeason, Long> {
    List<ClothingSeason> findByClothingClothingIdOrderByClothingSeasonId(Long clothingId);

    void deleteByClothingClothingId(Long clothingId);
}
