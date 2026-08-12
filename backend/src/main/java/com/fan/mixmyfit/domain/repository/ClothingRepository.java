package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.Clothing;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingRepository extends JpaRepository<Clothing, Long> {
}
