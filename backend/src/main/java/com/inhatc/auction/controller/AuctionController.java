package com.inhatc.auction.controller;

import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.dto.AuctionRequestDTO;
import com.inhatc.auction.service.AuctionService;

import jakarta.validation.Valid;
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

    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuctionDetail(@PathVariable Long auctionId) {
        return ResponseEntity.ok(auctionService.getAuctionDetail(auctionId));
    }

    @PostMapping
    public ResponseEntity<?> createAuction(@Valid @ModelAttribute AuctionRequestDTO requestDTO) {
        Long auctionId = this.auctionService.createAuction(requestDTO);

        HashMap<String, Long> response = new HashMap<>();
        response.put("auctionId", auctionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    //////////////////////////////////////////////////////////////////////////////////////
    // @GetMapping("/{auctionId}/bids") // 입찰 내역 조회
    // public ResponseEntity<?> getAuctionBidList(@PathVariable Long auctionId) {
    // return ResponseEntity.ok(auctionService.getAuctionBidList(auctionId));
    // }

    // @PostMapping("/{auctionId}/bids") // 입찰 생성
    // public ResponseEntity<?> createAuctionBid(@PathVariable Long auctionId,
    // @Valid @RequestBody AuctionBidRequestDTO requestDTO) {
    // return ResponseEntity.ok(auctionService.createAuctionBid(auctionId,
    // requestDTO));
    // }

    // @PostMapping("/{auctionId}/purchase") // 즉시 구매
    // public ResponseEntity<?> purchaseAuction(@PathVariable Long auctionId) {
    // return ResponseEntity.ok(auctionService.purchaseAuction(auctionId));
    // }

    // @PatchMapping("/{auctionId}/status") // 경매 상태 변경
    // public ResponseEntity<?> updateAuctionStatus(@PathVariable Long auctionId,
    // @RequestBody AuctionStatusRequestDTO requestDTO) {
    // return ResponseEntity.ok(auctionService.updateAuctionStatus(auctionId,
    // requestDTO));
    // }
}
