package com.inhatc.auction.repository;

import com.inhatc.auction.domain.Auth;
import com.inhatc.auction.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Boolean existsByUser(User user);
    Optional<Auth> findByRefreshToken(String refreshToken);
}
