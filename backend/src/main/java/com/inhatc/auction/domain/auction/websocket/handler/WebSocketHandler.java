package com.inhatc.auction.domain.auction.websocket.handler;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.auction.websocket.dto.WebSocketRequestDTO;
import com.inhatc.auction.domain.auction.websocket.dto.WebSocketResponseDTO;
import com.inhatc.auction.domain.bid.entity.RedisBid;
import com.inhatc.auction.domain.bid.repository.RedisBidRepository;
import com.inhatc.auction.domain.transaction.entity.Transaction;
import com.inhatc.auction.domain.transaction.repository.TransactionRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.constant.AuctionStatus;
import com.inhatc.auction.global.constant.TransactionStatus;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

import io.jsonwebtoken.ExpiredJwtException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final RedisBidRepository redisBidRepository;
    private final TransactionRepository transactionRepository;

    private final Map<Long, Set<WebSocketSession>> auctionRooms = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override // 웹 소켓 연결시
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long auctionId = getAuctionId(session);
        Set<WebSocketSession> auctionRoom = auctionRooms.computeIfAbsent(auctionId,
                key -> new CopyOnWriteArraySet<>());

        // 세션 추가
        auctionRoom.add(session);
    }

    @Transactional // 데이터 통신시
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long auctionId = getAuctionId(session);
        Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);

        WebSocketRequestDTO request = objectMapper.readValue(message.getPayload(), WebSocketRequestDTO.class);
        String type = request.getType();
        Map<String, String> data = request.getData();
        String accessToken = request.getAccessToken();

        if (accessToken == null) {
            sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
            return;
        }

        try {
            if (!jwtTokenProvider.validateToken(accessToken)) {
                sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해주세요.");
                return;
            }
        } catch (ExpiredJwtException e) {
            sendToOne(session, "token_expired", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return;
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 사용자 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            sendToOne(session, "error", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
            return;
        }

        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) {
            sendToOne(session, "error", HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다.");
            return;
        }

        // 현재 경매가 종료된 경우
        if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "종료된 경매에는 입찰할 수 없습니다.");
            return;
        }

        if (type.equals("bid")) {
            // 입찰하려는 경매가 내가 등록한 경매인 경우
            if (auction.getUser().getId().equals(user.getId())) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "내가 등록한 경매에는 입찰할 수 없습니다.");
                return;
            }

            // 입찰 금액 (String -> Long)
            Long bidAmount = Long.valueOf(data.get("bidAmount"));

            // 최고 입찰자 조회
            List<RedisBid> redisBids = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auctionId);
            Boolean isFirstBid = redisBids.isEmpty();

            // 첫 입찰의 경우 시작가와 같거나 높아야 함
            if (isFirstBid) {
                if (bidAmount < auction.getStartPrice()) {
                    sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 시작가와 같거나 높아야 합니다");
                    return;
                }
            } else {
                // 현재 최고입찰자가 본인인 경우 입찰할 수 없음
                RedisBid highestBid = redisBids.get(0);
                if (highestBid.getUserId().equals(user.getId())) {
                    sendToOne(session, "error", HttpStatus.BAD_REQUEST, "현재 고객님이 최고입찰자입니다.");
                    return;
                }

                // 첫 입찰이 아닌 경우 현재 경매 가격보다 높아야 함
                if (bidAmount <= auction.getCurrentPrice()) {
                    sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액을 현재 경매 가격보다 높게 입력해주세요.");
                    return;
                }
            }

            // 입찰 저장
            RedisBid newBid = RedisBid.builder()
                    .auctionId(auctionId)
                    .userId(userId)
                    .bidAmount(bidAmount)
                    .bidTime(LocalDateTime.now())
                    .build();

            redisBidRepository.save(newBid);

            // 현재 경매 가격 업데이트
            auction.updateCurrentPrice(bidAmount);
            this.auctionRepository.save(auction);

            // 경매 남은 시간
            Long auctionLeftTime = Math.max(0,
                    Duration.between(LocalDateTime.now(), auction.getAuctionEndTime()).toSeconds());

            // 입찰 데이터
            WebSocketResponseDTO.BidData bidData = WebSocketResponseDTO.BidData.builder()
                    .userId(newBid.getUserId())
                    .nickname(user.getNickname())
                    .bidAmount(newBid.getBidAmount())
                    .createdAt(newBid.getBidTime().toString())
                    .auctionLeftTime(auctionLeftTime)
                    .build();

            // 메시지 + 입찰 데이터
            WebSocketResponseDTO.BidResponse bidResponse = WebSocketResponseDTO.BidResponse.builder()
                    .message("입찰이 완료되었습니다.")
                    .bidData(bidData)
                    .build();

            // 경매 방에 있는 모든 사용자에게 메시지 전송
            sendToAll(auctionRoom, "bid", HttpStatus.CREATED, bidResponse);

        } else if (type.equals("buy-now")) {
            // 즉시 구매가 가능한 경매인지 확인
            if (auction.getBuyNowPrice() == 0) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "즉시 구매가 가능한 경매가 아닙니다.");
                return;
            }

            // 즉시 구매하려는 경매가 내가 등록한 경매인 경우
            if (auction.getUser().getId().equals(user.getId())) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "내가 등록한 경매에는 즉시 구매할 수 없습니다.");
                return;
            }

            if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "종료된 경매에는 즉시 구매할 수 없습니다.");
                return;
            }

            // 경매 (종료 시간, 최종 낙찰가, 상태) 업데이트
            auction.updateAuctionEndTime(LocalDateTime.now());
            auction.setSuccessfulPrice(auction.getBuyNowPrice());
            auction.updateStatus(AuctionStatus.ENDED);

            // 최종 거래 내역 저장
            Transaction transaction = Transaction.builder()
                    .auction(auction)
                    .seller(auction.getUser())
                    .buyer(user)
                    .finalPrice(auction.getBuyNowPrice())
                    .status(TransactionStatus.COMPLETED)
                    .build();

            this.transactionRepository.save(transaction);
            this.auctionRepository.save(auction);

            // 즉시 구매 데이터
            WebSocketResponseDTO.BuyNowData buyNowData = WebSocketResponseDTO.BuyNowData.builder()
                    .userId(user.getId())
                    .nickname(user.getNickname())
                    .status(TransactionStatus.COMPLETED.toString())
                    .buyNowPrice(auction.getBuyNowPrice())
                    .build();

            // 메시지 + 즉시 구매 데이터
            WebSocketResponseDTO.BuyNowResponse buyNowResponse = WebSocketResponseDTO.BuyNowResponse.builder()
                    .message("즉시 구매가 완료되었습니다.")
                    .buyNowData(buyNowData)
                    .build();

            sendToAll(auctionRoom, "buy-now", HttpStatus.CREATED, buyNowResponse);
        }
    }

    // 경매 종료 알림 전송
    public void broadcastEnded(Auction auction) {
        Set<WebSocketSession> auctionRoom = auctionRooms.get(auction.getId());

        // Redis에서 최고 입찰 정보 조회
        List<RedisBid> highestBids = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId());

        // 입찰 내역이 없는 경우 처리
        if (highestBids.isEmpty()) {
            WebSocketResponseDTO.TransactionResponse transactionResponse = WebSocketResponseDTO.TransactionResponse
                    .builder()
                    .message("입찰자가 없어 경매가 종료되었습니다.")
                    .build();

            sendToAll(auctionRoom, "ended", HttpStatus.OK, transactionResponse);
            return;
        }

        // 최고 입찰자 정보 조회
        RedisBid highestBid = highestBids.get(0);
        User highestBidder = userRepository.findById(highestBid.getUserId())
                .orElseThrow(() -> new IllegalStateException("최고 입찰자를 찾을 수 없습니다."));

        WebSocketResponseDTO.TransactionData transactionData = WebSocketResponseDTO.TransactionData.builder()
                .userId(highestBidder.getId())
                .nickname(highestBidder.getNickname())
                .status(TransactionStatus.COMPLETED)
                .finalPrice(auction.getSuccessfulPrice())
                .build();

        WebSocketResponseDTO.TransactionResponse transactionResponse = WebSocketResponseDTO.TransactionResponse
                .builder()
                .message("경매가 종료되었습니다.")
                .transactionData(transactionData)
                .build();

        sendToAll(auctionRoom, "ended", HttpStatus.OK, transactionResponse);
    }

    // 경매 남은 시간 전송
    @Scheduled(fixedRate = 60000)
    public void sendRemainingTime() {
        for (Map.Entry<Long, Set<WebSocketSession>> entry : auctionRooms.entrySet()) {
            Long auctionId = entry.getKey();
            Set<WebSocketSession> auctionRoom = entry.getValue();

            // 경매 남은 시간
            Long auctionLeftTime = Math.max(auctionRepository.calculateAuctionLeftTime(auctionId), 0L);

            // 메시지 전송
            WebSocketResponseDTO.AuctionLeftTimeResponse auctionLeftTimeResponse = WebSocketResponseDTO.AuctionLeftTimeResponse
                    .builder()
                    .auctionLeftTime(auctionLeftTime)
                    .build();

            sendToAll(auctionRoom, "time", HttpStatus.OK, auctionLeftTimeResponse);
        }
    }

    // 개별 메시지 전송
    private void sendToOne(WebSocketSession session, String type, HttpStatus status, String message)
            throws IOException {
        WebSocketResponseDTO.Message msg = WebSocketResponseDTO.Message.builder()
                .message(message)
                .build();

        WebSocketResponseDTO response = WebSocketResponseDTO.builder()
                .type(type)
                .status(status.value())
                .data(msg)
                .build();

        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
    }

    // 단체 메시지 전송
    private void sendToAll(Set<WebSocketSession> auctionRoom, String type, HttpStatus status, Object data) {
        WebSocketResponseDTO response = WebSocketResponseDTO.builder()
                .type(type)
                .status(status.value())
                .data(data)
                .build();

        if (auctionRoom != null) {
            for (WebSocketSession session : auctionRoom) {
                try {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
                } catch (IOException e) {
                    log.error("웹소켓 통신 에러 [세션ID: {}] : {}", session.getId(), e.getMessage(), e);
                }
            }
        }
    }

    // 경매 ID 추출
    private Long getAuctionId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            throw new IllegalStateException("웹소켓 URI가 null입니다");
        }

        String path = uri.getPath();
        String[] pathParts = path.split("/");
        if (pathParts.length < 4) {
            throw new IllegalStateException("잘못된 웹소켓 경로입니다: " + path);
        }

        try {
            return Long.valueOf(pathParts[3]); // localhost:8080/ws/auctions/{auctionId}
        } catch (NumberFormatException e) {
            throw new IllegalStateException("경매 ID가 유효한 숫자가 아닙니다: " + pathParts[3]);
        }
    }

    @Override // 웹소켓 통신 에러시
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("웹소켓 통신 에러 [세션ID: {}] : {}", session.getId(), exception.getMessage(), exception);
    }

    @Override // 웹 소켓 연결 종료시
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        Long auctionId = getAuctionId(session);
        Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);
        if (auctionRoom != null) {
            auctionRoom.remove(session);
            if (auctionRoom.isEmpty()) {
                auctionRooms.remove(auctionId);
            }
        }
    }
}