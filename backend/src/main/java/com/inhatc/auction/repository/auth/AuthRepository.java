package com.inhatc.auction.repository.auth;

import com.inhatc.auction.domain.auth.Auth;
import com.inhatc.auction.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Boolean existsByUser(User user);
    Optional<Auth> findByRefreshToken(String refreshToken);
}
