package com.inhatc.auction.domain.sse.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.sse.dto.response.SseBidResponseDTO;
import com.inhatc.auction.domain.sse.dto.response.SseTransactionResponseDTO;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class SseEmitterService {
    // auctionId를 키로 하는 SseEmitter 목록 저장소
    // ConcurrentHashMap : 동시성을 지원하는 맵
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();
    private final Long DEFAULT_TIMEOUT = 1000L * 60 * 60;
    private final AuctionRepository auctionRepository;

    // SSE 연결 생성
    public SseEmitter subscribe(Long auctionId) throws IOException {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);

        try {
            // 초기 연결 확인용 이벤트
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data(auctionId + "번 경매에 연결되었습니다."));

            // 경매 아이디를 키로 하는 연결 목록 가져오기
            // computeIfAbsent : auctionEmitters 에 해당 auctionId 가 없으면 새로운 목록 생성
            List<SseEmitter> auctionEmitters = emitters.computeIfAbsent(auctionId,
                    keys -> new CopyOnWriteArrayList<>());

            auctionEmitters.add(emitter);

            // 연결 종료 시 정리
            emitter.onCompletion(() -> {
                removeEmitterQuietly(auctionId, emitter);
            });

            // 타임아웃 시 정리
            emitter.onTimeout(() -> {
                emitter.complete();
                removeEmitterQuietly(auctionId, emitter);
            });

            // 에러 발생 시 정리
            emitter.onError(ex -> {
                removeEmitterQuietly(auctionId, emitter);
            });

        } catch (IOException e) {
            log.debug("초기 연결 이벤트 실패", e);
            removeEmitterQuietly(auctionId, emitter);
        }

        return emitter;
    }

    // (SSE -> bid 이벤트) 특정 경매의 모든 구독자에게 입찰 데이터 전송
    public void broadcastBid(Long auctionId, SseBidResponseDTO sseBidResponseDTO) {
        List<SseEmitter> auctionEmitters = emitters.get(auctionId);
        if (auctionEmitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            auctionEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event()
                            .name("bid")
                            .data(sseBidResponseDTO));
                } catch (Exception e) {
                    deadEmitters.add(emitter);
                    try {
                        emitter.complete();
                    } catch (Exception ex) {
                        log.debug("SSE 연결 종료 중 오류 발생", ex);
                    }
                }
            });

            // 죽은 연결 제거
            deadEmitters.forEach(emitter -> removeEmitterQuietly(auctionId, emitter));
        }
    }

    // (SSE -> buyNow 이벤트) 특정 경매의 모든 구독자에게 경매 종료 알림 전송
    public void broadcastBuyNow(Long auctionId, SseTransactionResponseDTO sseTransactionResponseDTO) {
        List<SseEmitter> auctionEmitters = emitters.get(auctionId);
        if (auctionEmitters != null) {
            List<SseEmitter> deadEmitters = new ArrayList<>();

            auctionEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("buy-now").data(sseTransactionResponseDTO));
                    emitter.complete();
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                    try {
                        emitter.complete();
                    } catch (Exception ex) {
                        log.debug("SSE 연결 종료 중 오류 발생", ex);
                    }
                }
            });

            // 죽은 연결 제거
            deadEmitters.forEach(emitter -> removeEmitterQuietly(auctionId, emitter));

            // 해당 경매의 모든 연결이 끊어졌다면 목록에서 제거하기 위해 표시
            if (auctionEmitters.isEmpty()) {
                emitters.remove(auctionId);
            }
        }
    }

    // 10초마다 서버 시간 전송
    @Scheduled(fixedRate = 10000)
    public void sendServerTime() {
        List<Long> emptyAuctions = new ArrayList<>();

        emitters.forEach((auctionId, auctionEmitters) -> {
            // 경매 남은 시간을 전송
            Long auctionLeftTime = auctionRepository.calculateAuctionLeftTime(auctionId) > 0
                    ? auctionRepository.calculateAuctionLeftTime(auctionId)
                    : 0;

            List<SseEmitter> deadEmitters = new ArrayList<>();

            auctionEmitters.forEach(emitter -> {
                try {
                    emitter.send(SseEmitter.event().name("time").data(auctionLeftTime));
                } catch (IOException e) {
                    deadEmitters.add(emitter);

                    try {
                        emitter.complete();
                    } catch (Exception ex) {
                        log.debug("SSE 연결 종료 중 오류 발생", ex);
                    }
                }
            });

            // 죽은 연결 제거
            auctionEmitters.removeAll(deadEmitters);

            // 해당 경매의 모든 연결이 끊어졌다면 목록에서 제거하기 위해 표시
            if (auctionEmitters.isEmpty()) {
                emptyAuctions.add(auctionId);
            }

            // 빈 경매 목록에서 제거
            emptyAuctions.forEach(emitters::remove);
        });
    }

    // 조용히 emitter 제거 (예외 발생하지 않음)
    private void removeEmitterQuietly(Long auctionId, SseEmitter emitter) {
        try {
            List<SseEmitter> auctionEmitters = emitters.get(auctionId);
            if (auctionEmitters != null) {
                auctionEmitters.remove(emitter);
                if (auctionEmitters.isEmpty()) {
                    emitters.remove(auctionId);
                }
            }
        } catch (Exception e) {
            log.debug("SSE 연결 제거 중 오류 발생", e);
        }
    }

    // 모든 연결 종료 (애플리케이션 종료 시 사용)
    @PreDestroy
    public void shutdown() {
        emitters.forEach((auctionId, emitterList) -> {
            emitterList.forEach(emitter -> {
                try {
                    emitter.complete();
                } catch (Exception e) {
                    log.error("SSE 연결 종료 중 오류 발생", e);
                }
            });
        });
        emitters.clear();
    }
}
