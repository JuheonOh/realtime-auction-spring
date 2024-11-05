package com.inhatc.auction.domain.auction.websocket;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
import com.inhatc.auction.domain.transaction.entity.Transaction;
import com.inhatc.auction.domain.transaction.repository.TransactionRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.constant.AuctionStatus;
import com.inhatc.auction.global.constant.TransactionStatus;
import com.inhatc.auction.global.security.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final TransactionRepository transactionRepository;

    private final Map<Long, Set<WebSocketSession>> auctionRooms = new ConcurrentHashMap<>();

    @Override // 웹 소켓 연결시
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        Long auctionId = getAuctionId(session);
        Set<WebSocketSession> auctionRoom = auctionRooms.computeIfAbsent(auctionId,
                key -> new CopyOnWriteArraySet<>());

        // 세션 추가
        auctionRoom.add(session);
    }

    @SuppressWarnings("unchecked")
    @Override // 데이터 통신시
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long auctionId = getAuctionId(session);
        JSONParser parser = new JSONParser();
        JSONObject payload = (JSONObject) parser.parse(message.getPayload());

        String type = (String) payload.get("type");
        Object data = payload.get("data");
        String accessToken = (String) payload.get("accessToken");

        JSONObject response = new JSONObject();
        if (accessToken == null) {
            response.put("type", "error");
            response.put("status", 400);
            response.put("message", "로그인이 필요합니다.");
            session.sendMessage(new TextMessage(response.toString()));
            return;
        }

        if (!jwtTokenProvider.validateToken(accessToken)) {
            response.put("type", "error");
            response.put("status", 400);
            response.put("message", "유효하지 않은 토큰입니다. 다시 로그인해주세요.");
            session.sendMessage(new TextMessage(response.toString()));
            return;
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

        // 사용자 조회
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            response.put("type", "error");
            response.put("status", 400);
            response.put("message", "사용자를 찾을 수 없습니다.");
            session.sendMessage(new TextMessage(response.toString()));
            return;
        }

        // 경매 조회
        Auction auction = auctionRepository.findById(auctionId).orElse(null);
        if (auction == null) {
            response.put("type", "error");
            response.put("status", 400);
            response.put("message", "경매를 찾을 수 없습니다.");
            session.sendMessage(new TextMessage(response.toString()));
            return;
        }

        // 현재 경매가 종료된 경우
        if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
            response.put("type", "error");
            response.put("status", 400);
            response.put("message", "종료된 경매에는 입찰할 수 없습니다.");
            session.sendMessage(new TextMessage(response.toString()));
            return;
        }

        if (type.equals("bid")) {
            // 입찰하려는 경매가 내가 등록한 경매인 경우
            if (auction.getUser().getId() == user.getId()) {
                response.put("type", "error");
                response.put("status", 400);
                response.put("message", "내가 등록한 경매에는 입찰할 수 없습니다.");
                session.sendMessage(new TextMessage(response.toString()));
                return;
            }

            // 입찰 금액
            Long bidAmount = (Long) data;

            // 입찰 횟수 조회
            Boolean isFirstBid = bidRepository.findBidCountByAuctionId(auctionId).orElse(null) == 0;

            // 현재 최고입찰자가 본인인 경우 입찰할 수 없음
            Long currentHighestBidUserId = this.bidRepository.findCurrentHighestBidUserId(auctionId).orElse(null);
            if (currentHighestBidUserId != null && currentHighestBidUserId == user.getId()) {
                response.put("type", "error");
                response.put("status", 400);
                response.put("message", "현재 고객님이 최고입찰자입니다.");
                session.sendMessage(new TextMessage(response.toString()));
                return;
            }

            // 첫 입찰의 경우 시작가와 같거나 높아야 함
            if (isFirstBid) {
                if (bidAmount < auction.getStartPrice()) {
                    response.put("status", 400);
                    response.put("message", "입찰 금액이 시작가와 같거나 높아야 합니다");
                    session.sendMessage(new TextMessage(response.toString()));
                    return;
                }
            } else {
                // 첫 입찰이 아닌 경우 현재 경매 가격보다 높아야 함
                if (bidAmount <= auction.getCurrentPrice()) {
                    response.put("status", 400);
                    response.put("message", "입찰 금액을 현재 경매 가격보다 높게 입력해주세요.");
                    session.sendMessage(new TextMessage(response.toString()));
                    return;
                }
            }

            // 입찰 저장
            Bid bid = Bid.builder()
                    .auction(auction)
                    .user(user)
                    .bidAmount(bidAmount)
                    .build();

            bidRepository.save(bid);

            // 현재 경매 가격 업데이트
            auction.updateCurrentPrice(bidAmount);
            this.auctionRepository.save(auction);

            // 경매 남은 시간
            Long auctionLeftTime = Math.max(0,
                    Duration.between(LocalDateTime.now(), auction.getAuctionEndTime()).toSeconds());

            JSONObject bidData = new JSONObject();
            bidData.put("id", bid.getId());
            bidData.put("userId", bid.getUser().getId());
            bidData.put("nickname", bid.getUser().getNickname());
            bidData.put("bidAmount", bid.getBidAmount());
            bidData.put("createdAt", bid.getCreatedAt().toString());
            bidData.put("auctionLeftTime", auctionLeftTime);

            response.clear();
            response.put("type", "bid");
            response.put("userId", bid.getUser().getId());
            response.put("status", 201);
            response.put("message", "입찰이 완료되었습니다.");
            response.put("bidData", bidData);

            Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);
            if (auctionRoom != null) {
                for (WebSocketSession s : auctionRoom) {
                    try {
                        s.sendMessage(new TextMessage(response.toString()));
                    } catch (IOException e) {
                        log.error("웹소켓 통신 에러 : {}", e.getMessage());
                    }
                }
            }
        } else if (type.equals("buy-now")) {
            // log.info("즉시 구매 요청한 사용자 닉네임 : {}", user.getNickname());

            if (auction.getUser().getId() == user.getId()) {
                response.put("type", "error");
                response.put("status", 400);
                response.put("message", "내가 등록한 경매에는 즉시 구매할 수 없습니다.");
                session.sendMessage(new TextMessage(response.toString()));
                return;
            }

            if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
                response.put("type", "error");
                response.put("status", 400);
                response.put("message", "종료된 경매에는 즉시 구매할 수 없습니다.");
                session.sendMessage(new TextMessage(response.toString()));
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

            // 경매 종료 알림

            JSONObject buyNowData = new JSONObject();
            buyNowData.put("userId", user.getId());
            buyNowData.put("nickname", user.getNickname());
            buyNowData.put("finalPrice", auction.getBuyNowPrice());
            buyNowData.put("status", TransactionStatus.COMPLETED.toString());

            response.clear();
            response.put("type", "buy-now");
            response.put("status", 201);
            response.put("message", "즉시 구매가 완료되었습니다.");
            response.put("data", buyNowData);

            Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);
            if (auctionRoom != null) {
                for (WebSocketSession s : auctionRoom) {
                    try {
                        s.sendMessage(new TextMessage(response.toString()));
                    } catch (IOException e) {
                        log.error("웹소켓 통신 에러 : {}", e.getMessage());
                    }
                }
            }
        }
    }

    // 경매 남은 시간 전송
    @SuppressWarnings("unchecked")
    @Scheduled(fixedRate = 60000)
    public void sendRemainingTime() {
        for (Map.Entry<Long, Set<WebSocketSession>> entry : auctionRooms.entrySet()) {
            Long auctionId = entry.getKey();
            Set<WebSocketSession> auctionRoom = entry.getValue();

            // 경매 남은 시간
            Long auctionLeftTime = Math.max(auctionRepository.calculateAuctionLeftTime(auctionId), 0L);

            // 메시지 전송
            JSONObject data = new JSONObject();
            data.put("auctionLeftTime", auctionLeftTime);

            JSONObject jsonObject = new JSONObject();
            jsonObject.put("type", "time");
            jsonObject.put("data", data);

            for (WebSocketSession session : auctionRoom) {
                try {
                    session.sendMessage(new TextMessage(jsonObject.toString()));
                } catch (IOException e) {
                    log.error("경매 시간 업데이트 전송 중 에러 : {}", e.getMessage());
                }
            }
        }
    }

    @Override // 웹소켓 통신 에러시
    public void handleTransportError(@NonNull WebSocketSession session, @NonNull Throwable exception) throws Exception {
        log.error("웹소켓 통신 에러 : {}", exception.getMessage());
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
            return Long.parseLong(pathParts[3]); // /api/auctions/{auctionId}/ws
        } catch (NumberFormatException e) {
            throw new IllegalStateException("경매 ID가 유효한 숫자가 아닙니다: " + pathParts[3]);
        }
    }
}
