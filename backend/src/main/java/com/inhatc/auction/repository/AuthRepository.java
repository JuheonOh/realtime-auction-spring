package com.inhatc.auction.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.Auth;
import com.inhatc.auction.domain.User;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Boolean existsByUser(User user);

    @Query("SELECT a.user FROM Auth a WHERE a.accessToken = :accessToken")
    Optional<User> findUserByAccessToken(@Param("accessToken") String accessToken);

    Optional<Auth> findByRefreshToken(String refreshToken);
}
