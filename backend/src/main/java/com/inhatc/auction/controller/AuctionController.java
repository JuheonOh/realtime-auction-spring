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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auctions")
public class AuctionController {
    private final AuctionService auctionService;

    // 경매 목록 조회
    @GetMapping
    public ResponseEntity<?> getAuctionList() {
        return ResponseEntity.status(HttpStatus.OK).body(auctionService.getAuctionList());
    }

    // 주목할 만한 경매 조회
    @GetMapping("/featured")
    public ResponseEntity<?> getFeaturedAuctions() {
        return ResponseEntity.status(HttpStatus.OK).body(auctionService.getFeaturedAuctionList());
    }

    // 경매 상세 조회
    @GetMapping("/{auctionId}")
    public ResponseEntity<?> getAuctionDetail(@PathVariable("auctionId") Long auctionId) {
        return ResponseEntity.status(HttpStatus.OK).body(auctionService.getAuctionDetail(auctionId));
    }

    // 경매 생성
    @PostMapping
    public ResponseEntity<?> createAuction(@Valid @ModelAttribute AuctionRequestDTO requestDTO) {
        Long auctionId = this.auctionService.createAuction(requestDTO);

        HashMap<String, Long> response = new HashMap<>();
        response.put("auctionId", auctionId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // 즉시 구매
    @PostMapping("/{auctionId}/buy-now")
    public ResponseEntity<?> buyNowAuction(HttpServletRequest request, @PathVariable("auctionId") Long auctionId) {
        auctionService.buyNowAuction(request, auctionId);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
