package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Clothing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingRepository extends JpaRepository<Clothing, Long> {
    List<Clothing> findByUserUserIdOrderByClothingId(Long userId);
}
