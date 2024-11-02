package com.inhatc.auction.domain.bid.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.domain.bid.dto.request.BidRequestDTO;
import com.inhatc.auction.domain.bid.service.BidService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Log4j2
public class BidController {
    private final BidService bidService;

    // 입찰 내역 조회
    @GetMapping("/bids/{auctionId}")
    public ResponseEntity<?> getBidList(@PathVariable("auctionId") Long auctionId) {
        return ResponseEntity.ok(bidService.getBidList(auctionId));
    }

    // 입찰 생성
    @PostMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<?> createBid(HttpServletRequest request,
            @PathVariable("auctionId") Long auctionId, @Valid @RequestBody BidRequestDTO requestDTO) {
        bidService.createBid(request, auctionId, requestDTO);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
