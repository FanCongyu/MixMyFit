package com.fan.mixmyfit.domain.repository;

import com.fan.mixmyfit.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
