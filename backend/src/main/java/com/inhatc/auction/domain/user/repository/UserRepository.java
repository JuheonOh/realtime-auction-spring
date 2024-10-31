package com.inhatc.auction.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.inhatc.auction.domain.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Boolean existsByEmail(String email);

    Boolean existsByNickname(String nickname);

    Optional<User> findByEmail(String email);

    Optional<User> findByName(String username);

    @Query("SELECT u.nickname FROM User u WHERE u.id = :userId")
    String findNicknameById(@Param("userId") Long userId);
}
