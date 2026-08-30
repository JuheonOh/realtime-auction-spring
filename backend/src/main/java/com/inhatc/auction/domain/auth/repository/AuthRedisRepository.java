package com.inhatc.auction.domain.auth.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import com.inhatc.auction.domain.auth.entity.Auth;

@Repository
public interface AuthRedisRepository extends CrudRepository<Auth, String> {
    List<Auth> findAllByUserId(Long userId);
}
