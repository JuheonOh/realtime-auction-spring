package com.inhatc.auction.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.auth.entity.Auth;
import com.inhatc.auction.domain.user.entity.User;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Boolean existsByUser(User user);

    @Query("SELECT a.user FROM Auth a WHERE a.accessToken = :accessToken")
    Optional<User> findUserByAccessToken(@Param("accessToken") String accessToken);

    Optional<Auth> findByRefreshToken(String refreshToken);
}
