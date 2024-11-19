package com.inhatc.auction.domain.auth.repository;

import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inhatc.auction.domain.auth.entity.Auth;

@Repository
public interface AuthRedisRepository extends CrudRepository<Auth, Long> {
    Optional<Auth> findByRefreshToken(String refreshToken);
}
