package com.inhatc.auction.domain.redis.migration.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.domain.redis.migration.service.BidMigrationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class BidMigrationController {
    private final BidMigrationService bidMigrationService;

    @GetMapping("/bids")
    public ResponseEntity<String> migrateBids(@RequestParam(name = "target", required = true) String target) {
        switch (target) {
            case "redis":
                bidMigrationService.migrateAllBidsToRedis();
                break;
            case "mariadb":
                bidMigrationService.migrateAllBidsToMariaDB();
                break;
        }

        return ResponseEntity.ok("마이그레이션이 완료되었습니다.");
    }
}