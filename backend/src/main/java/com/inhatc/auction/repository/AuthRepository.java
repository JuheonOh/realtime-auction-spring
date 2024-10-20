package com.inhatc.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.inhatc.auction.domain.Auth;
import com.inhatc.auction.domain.User;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Boolean existsByUser(User user);

    Optional<Auth> findByRefreshToken(String refreshToken);
}
