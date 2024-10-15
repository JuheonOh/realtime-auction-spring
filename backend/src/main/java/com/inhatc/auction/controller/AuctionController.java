package com.inhatc.auction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.dto.AuctionRequestDTO;
import com.inhatc.auction.service.AuctionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auctions")
public class AuctionController {
    private final AuctionService auctionService;

    @GetMapping
    public ResponseEntity<?> getAuctionList() {
        return ResponseEntity.ok(auctionService.getAuctionList());
    }

    @PostMapping
    public ResponseEntity<?> createAuction(@ModelAttribute AuctionRequestDTO requestDTO) {
        this.auctionService.createAuction(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }
}
