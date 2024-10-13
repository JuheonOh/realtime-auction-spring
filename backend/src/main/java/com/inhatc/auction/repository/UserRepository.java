package com.inhatc.auction.repository;

import com.inhatc.auction.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByName(String username);
}
