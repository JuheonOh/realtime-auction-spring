package com.inhatc.auction.domain.sse.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.inhatc.auction.domain.sse.service.SseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class SseController {
    private final SseService sseEmitterService;

    // 입찰 스트림 조회
    @GetMapping(value = "/auctions/{auctionId}/bids-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getBidStream(@PathVariable("auctionId") Long auctionId) throws Exception {
        return sseEmitterService.subscribe(auctionId);
    }
}
