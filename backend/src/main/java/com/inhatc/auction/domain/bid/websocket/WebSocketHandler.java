package com.inhatc.auction.domain.bid.websocket;

import java.io.IOException;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.dto.request.WebSocketRequestDTO;
import com.inhatc.auction.domain.bid.dto.response.WebSocketResponseDTO;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.MyBidInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.dto.response.PreviousBidInfoDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.repository.NotificationRepository;
import com.inhatc.auction.domain.transaction.entity.TransactionStatus;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.jwt.JwtTokenProvider;
import com.inhatc.auction.global.outbox.service.OutboxPublisher;
import com.inhatc.auction.global.utils.TimeUtils;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Component
@RequiredArgsConstructor
public class WebSocketHandler extends TextWebSocketHandler {

    private static final int MAX_MESSAGE_LENGTH = 16 * 1024;
    private static final int MAX_ACCESS_TOKEN_LENGTH = 4 * 1024;
    private static final String SESSION_AUCTION_ID = "webSocketAuctionId";
    private static final String SESSION_USER_ID = "webSocketUserId";

    private final JwtTokenProvider jwtTokenProvider;

    private final UserRepository userRepository;
    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final NotificationRepository notificationRepository;

    private final OutboxPublisher outboxPublisher;

    private final Map<Long, Set<WebSocketSession>> auctionRooms = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override // 웹 소켓 연결시
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        // 인증된 메시지를 받을 때까지 경매 방에 참여시키지 않는다.
    }

    @Transactional // 데이터 통신시
    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        Long auctionId = getAuctionId(session);

        WebSocketRequestDTO request = parseRequest(session, message);
        if (request == null) {
            return;
        }
        String type = request.getType();
        Map<String, String> data = request.getData();

        if (getLongAttribute(session.getAttributes(), SESSION_USER_ID) == null && !type.equals("auth")) {
            sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "웹소켓 인증 메시지가 먼저 필요합니다.");
            return;
        }

        User user = authenticateSession(session, auctionId, request.getAccessToken());
        if (user == null) {
            return;
        }
        Long userId = user.getId();
        Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);

        if (type.equals("auth")) {
            sendToOne(session, "auth", HttpStatus.OK, "웹소켓 인증이 완료되었습니다.");
            return;
        }

        // 경매 조회
        Auction auction = auctionRepository.findByIdForUpdate(auctionId).orElse(null);
        if (auction == null) {
            sendToOne(session, "error", HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다.");
            return;
        }

        // 현재 경매가 종료된 경우
        if (auction.getStatus() != com.inhatc.auction.domain.auction.entity.AuctionStatus.ACTIVE
                || !auction.getAuctionEndTime().isAfter(LocalDateTime.now())) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "종료된 경매에는 입찰할 수 없습니다.");
            return;
        }

        if (type.equals("bid")) {
            // 입찰하려는 경매가 내가 등록한 경매인 경우
            if (auction.getUser().getId().equals(user.getId())) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "내가 등록한 경매에는 입찰할 수 없습니다.");
                return;
            }

            Long bidAmount = parseBidAmount(session, data);
            if (bidAmount == null) {
                return;
            }

            // 최고 입찰자 조회
            List<Bid> bidList = this.bidRepository.findByAuctionId(auctionId);
            Bid highestBid = bidList.isEmpty() ? null : bidList.get(0);

            // 최고 입찰자가 없는 경우
            if (highestBid == null) {
                // 시작가와 같거나 높아야 함
                if (bidAmount < auction.getStartPrice() || bidAmount < auction.getCurrentPrice()) {
                    sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 시작가와 같거나 높아야 합니다");
                    return;
                }
            } else {
                Long highestBidderId = highestBid.getUser().getId();
                // 현재 최고입찰자가 본인인 경우 입찰할 수 없음
                if (highestBidderId.equals(user.getId())) {
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
            Bid newBid = Bid.builder()
                    .auction(auction)
                    .user(user)
                    .bidAmount(bidAmount)
                    .bidTime(LocalDateTime.now())
                    .build();

            this.bidRepository.save(Objects.requireNonNull(newBid));

            // 현재 경매 가격 업데이트
            auction.updateCurrentPrice(bidAmount);
            this.auctionRepository.save(auction);

            // 현재 입찰자에게 BID 알림 전송

            // 중복 알림 조회
            Optional<Notification> duplicateBidNotificationOptional = this.notificationRepository
                    .findDuplicatedNotification(userId, NotificationType.BID, auctionId);

            // BID 알림 생성 (현재 입찰자에게)
            Notification bidNotification;

            // 중복 알림이 있는 경우 삭제
            if (duplicateBidNotificationOptional.isPresent()) {
                Notification duplicateBidNotification = duplicateBidNotificationOptional.get();
                duplicateBidNotification.markAsDeleted();
                this.notificationRepository.save(duplicateBidNotification);
            }

            bidNotification = Notification.builder()
                    .type(NotificationType.BID)
                    .user(user)
                    .auctionId(auctionId)
                    .build();

            // 알림 저장
            this.notificationRepository.save(Objects.requireNonNull(bidNotification));

            // 해당 경매에 이전 알림 중 OUTBID 알림이 있는 경우 삭제
            Optional<Notification> duplicateOutbidNotificationOptional = this.notificationRepository
                    .findDuplicatedNotification(userId, NotificationType.OUTBID, auctionId);

            // 중복 알림이 있는 경우 삭제
            if (duplicateOutbidNotificationOptional.isPresent()) {
                Notification duplicateOutbidNotification = duplicateOutbidNotificationOptional.get();
                duplicateOutbidNotification.markAsDeleted();
                this.notificationRepository.save(Objects.requireNonNull(duplicateOutbidNotification));
            }

            // BID 알림 전송
            Long previousBidAmount = highestBid != null ? highestBid.getBidAmount() : null;
            PreviousBidInfoDTO previousBidInfoDTO = null;

            // 이전 최고 입찰자가 있는 경우
            if (previousBidAmount != null) {
                previousBidInfoDTO = PreviousBidInfoDTO.builder()
                        .bidAmount(previousBidAmount)
                        .build();
            }

            NotificationResponseDTO bidDTO = NotificationResponseDTO.builder()
                    .id(bidNotification.getId())
                    .type(NotificationType.BID)
                    .isRead(bidNotification.getIsRead())
                    .time(TimeUtils.getRelativeTimeString(bidNotification.getCreatedAt()))
                    .auctionInfo(AuctionInfoDTO.builder()
                            .id(auctionId)
                            .title(auction.getTitle())
                            .currentPrice(auction.getCurrentPrice())
                            .filePath(auction.getImages().get(0).getFilePath())
                            .fileName(auction.getImages().get(0).getFileName())
                            .auctionEndTime(auction.getAuctionEndTime())
                            .build())
                    .myBidInfo(MyBidInfoDTO.builder()
                            .bidAmount(bidAmount)
                            .build())
                    .previousBidInfo(previousBidInfoDTO)
                    .build();

            outboxPublisher.publishSse(userId, bidDTO);

            // 이전 최고 입찰자가 있는 경우 이전 최고 입찰자에게 OUTBID 알림
            if (highestBid != null) {
                Long previousBidderId = highestBid.getUser().getId();
                if (previousBidderId == null) {
                    log.warn("이전 최고입찰자 ID가 없어 OUTBID 알림을 생략합니다. auctionId={}", auctionId);
                } else {
                    User previousBidder = userRepository.findById(previousBidderId).orElse(null);
                    if (previousBidder == null) {
                        log.warn("이전 최고입찰자 조회 실패로 OUTBID 알림을 생략합니다. auctionId={}, userId={}", auctionId,
                                previousBidderId);
                    } else {
                        // OUTBID 알림 생성
                        Notification outbidNotification = Notification.builder()
                                .type(NotificationType.OUTBID)
                                .auctionId(auctionId)
                                .user(previousBidder)
                                .build();

                        // 중복 알림 조회
                        Optional<Notification> duplicateOutbidNotification = this.notificationRepository
                                .findDuplicatedNotification(previousBidderId, NotificationType.OUTBID, auctionId);

                        // 중복 알림이 있는 경우 삭제
                        if (duplicateOutbidNotification.isPresent()) {
                            Notification duplicateNotification = duplicateOutbidNotification.get();
                            duplicateNotification.markAsDeleted();
                            this.notificationRepository.save(Objects.requireNonNull(duplicateNotification));
                        }

                        // 알림 저장
                        this.notificationRepository.save(Objects.requireNonNull(outbidNotification));

                        // 마지막 입찰 정보
                        MyBidInfoDTO myBidInfoDTO = MyBidInfoDTO.builder()
                                .bidAmount(previousBidAmount)
                                .build();

                        // OUTBID 알림 전송
                        NotificationResponseDTO outbidDTO = NotificationResponseDTO.builder()
                                .id(outbidNotification.getId())
                                .type(outbidNotification.getType())
                                .isRead(outbidNotification.getIsRead())
                                .time(TimeUtils.getRelativeTimeString(outbidNotification.getCreatedAt()))
                                .auctionInfo(AuctionInfoDTO.builder()
                                        .id(auctionId)
                                        .title(auction.getTitle())
                                        .currentPrice(auction.getCurrentPrice())
                                        .filePath(auction.getImages().get(0).getFilePath())
                                        .fileName(auction.getImages().get(0).getFileName())
                                        .auctionEndTime(auction.getAuctionEndTime())
                                        .build())
                                .myBidInfo(myBidInfoDTO)
                                .build();

                        outboxPublisher.publishSse(previousBidderId, outbidDTO);
                    }
                }
            }

            // 입찰 데이터
            WebSocketResponseDTO.BidData bidData = WebSocketResponseDTO.BidData.builder()
                    .userId(newBid.getUser().getId())
                    .nickname(user.getNickname())
                    .bidAmount(newBid.getBidAmount())
                    .bidTime(newBid.getBidTime().toString())
                    .auctionLeftTime(auction.getAuctionLeftTime())
                    .build();

            // 메시지 + 입찰 데이터
            WebSocketResponseDTO.BidResponse bidResponse = WebSocketResponseDTO.BidResponse.builder()
                    .message("입찰이 완료되었습니다.")
                    .bidData(bidData)
                    .build();

            outboxPublisher.publishWebSocket(auctionId, "bid", HttpStatus.CREATED.value(),
                    objectMapper.valueToTree(bidResponse));

        } else if (type.equals("buy-now")) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "즉시 구매는 HTTP API를 통해 요청해주세요.");
        } else {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "지원하지 않는 메시지 타입입니다.");
            }
            }

    private User authenticateSession(WebSocketSession session, Long auctionId, String accessToken) throws IOException {
        Map<String, Object> attributes = session.getAttributes();
        Long boundUserId = getLongAttribute(attributes, SESSION_USER_ID);
        Long boundAuctionId = getLongAttribute(attributes, SESSION_AUCTION_ID);

        if (boundUserId == null || boundAuctionId == null) {
            if (accessToken == null) {
                sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
                return null;
            }

            Long userId = validateAccessToken(session, accessToken);
            if (userId == null) {
                return null;
            }
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                sendToOne(session, "error", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
                return null;
            }
            if (!auctionRepository.existsById(auctionId)) {
                sendToOne(session, "error", HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다.");
                return null;
            }

            synchronized (attributes) {
                if (attributes.containsKey(SESSION_USER_ID) || attributes.containsKey(SESSION_AUCTION_ID)) {
                    sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "웹소켓 인증 정보가 올바르지 않습니다.");
                    return null;
                }
                attributes.put(SESSION_USER_ID, userId);
                attributes.put(SESSION_AUCTION_ID, auctionId);
            }
            auctionRooms.computeIfAbsent(auctionId, key -> new CopyOnWriteArraySet<>()).add(session);
            return user;
        }

        if (!auctionId.equals(boundAuctionId)) {
            sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "웹소켓 인증 정보가 올바르지 않습니다.");
            return null;
        }
        if (accessToken != null) {
            Long tokenUserId = validateAccessToken(session, accessToken);
            if (tokenUserId == null || !boundUserId.equals(tokenUserId)) {
                if (tokenUserId != null) {
                    sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "다른 사용자 토큰은 사용할 수 없습니다.");
                }
                return null;
            }
        }

        User user = userRepository.findById(boundUserId).orElse(null);
        if (user == null) {
            sendToOne(session, "error", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.");
        }
        return user;
    }

    private Long validateAccessToken(WebSocketSession session, String accessToken) throws IOException {
        try {
            if (!jwtTokenProvider.validateToken(accessToken)) {
                sendToOne(session, "error", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해주세요.");
                return null;
            }
            return jwtTokenProvider.getUserIdFromToken(accessToken);
        } catch (ExpiredJwtException e) {
            sendToOne(session, "token_expired", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.");
            return null;
        }
    }

    private Long getLongAttribute(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        return value instanceof Long ? (Long) value : null;
    }

    public void broadcastOutbox(String eventId, Long auctionId, String type, int status, JsonNode data) {
        Set<WebSocketSession> auctionRoom = auctionRooms.get(auctionId);
        Object responseData;

        if ("buy-now".equals(type)) {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new IllegalStateException("Auction not found: " + auctionId));
            Long buyerId = data.path("buyerId").isIntegralNumber() ? data.path("buyerId").longValue() : null;
            User buyer = buyerId == null ? null : userRepository.findById(buyerId).orElse(null);
            if (buyer == null) {
                throw new IllegalStateException("Buy-now buyer not found: " + buyerId);
            }

            responseData = WebSocketResponseDTO.BuyNowResponse.builder()
                    .message("즉시 구매가 완료되었습니다.")
                    .buyNowData(WebSocketResponseDTO.BuyNowData.builder()
                            .userId(buyer.getId())
                            .nickname(buyer.getNickname())
                            .status(TransactionStatus.COMPLETED.toString())
                            .buyNowPrice(auction.getBuyNowPrice())
                            .build())
                    .build();
        } else if ("ended".equals(type)) {
            Auction auction = auctionRepository.findById(auctionId)
                    .orElseThrow(() -> new IllegalStateException("Auction not found: " + auctionId));
            responseData = createEndedResponse(auction);
        } else {
            responseData = data;
        }

        sendToAll(auctionRoom, eventId, type, status, responseData);
    }

    private WebSocketResponseDTO.TransactionResponse createEndedResponse(Auction auction) {
        List<Bid> bidList = bidRepository.findByAuctionId(auction.getId());
        Bid highestBid = bidList.isEmpty() ? null : bidList.get(0);

        if (highestBid == null) {
            return WebSocketResponseDTO.TransactionResponse
                    .builder()
                    .message("입찰자가 없어 경매가 종료되었습니다.")
                    .build();
        }

        Long highestBidderId = highestBid.getUser().getId();
        User highestBidder = userRepository.findById(highestBidderId).orElse(null);
        if (highestBidder != null) {
            WebSocketResponseDTO.TransactionData transactionData = WebSocketResponseDTO.TransactionData.builder()
                    .userId(highestBidder.getId())
                    .nickname(highestBidder.getNickname())
                    .status(TransactionStatus.COMPLETED)
                    .finalPrice(auction.getSuccessfulPrice())
                    .build();

            return WebSocketResponseDTO.TransactionResponse
                    .builder()
                    .message("경매가 종료되었습니다.")
                    .transactionData(transactionData)
                    .build();
        }
        throw new IllegalStateException("Highest bidder not found: " + highestBidderId);
    }

    // 경매 남은 시간 전송 (1분 마다)
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

        session.sendMessage(Objects.requireNonNull(toTextMessage(Objects.requireNonNull(response))));
    }

    // 단체 메시지 전송
    private void sendToAll(Set<WebSocketSession> auctionRoom, String type, HttpStatus status, Object data) {
        sendToAll(auctionRoom, null, type, status.value(), data);
    }

    private void sendToAll(Set<WebSocketSession> auctionRoom, String eventId, String type, int status, Object data) {
        WebSocketResponseDTO response = WebSocketResponseDTO.builder()
                .eventId(eventId)
                .type(type)
                .status(status)
                .data(data)
                .build();

        if (auctionRoom != null) {
            for (WebSocketSession session : auctionRoom) {
                try {
                    session.sendMessage(Objects.requireNonNull(toTextMessage(Objects.requireNonNull(response))));
                } catch (IOException e) {
                    log.error("웹소켓 통신 에러 [세션ID: {}] : {}", session.getId(), e.getMessage(), e);
                }
            }
        }
    }

    private WebSocketRequestDTO parseRequest(WebSocketSession session, TextMessage message) throws IOException {
        String payload = message.getPayload();
        if (payload == null || payload.isBlank() || payload.length() > MAX_MESSAGE_LENGTH) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            if (root == null || !root.isObject()
                    || !root.path("type").isTextual()
                    || !root.path("data").isObject()) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
                return null;
            }

            String type = root.get("type").textValue();
            JsonNode accessTokenNode = root.get("accessToken");
            String accessToken = accessTokenNode == null || accessTokenNode.isNull() ? null : accessTokenNode.textValue();
            if (type == null || type.isBlank() || type.length() > 32
                    || (accessTokenNode != null && !accessTokenNode.isNull() && !accessTokenNode.isTextual())
                    || (accessToken != null && accessToken.length() > MAX_ACCESS_TOKEN_LENGTH)) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
                return null;
            }

            Map<String, String> data = new HashMap<>();
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = root.get("data").fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (field.getKey().length() > 64 || !field.getValue().isTextual()
                        || field.getValue().textValue().length() > 64) {
                    sendToOne(session, "error", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
                    return null;
                }
                data.put(field.getKey(), field.getValue().textValue());
            }
            return new WebSocketRequestDTO(type, accessToken, data);
        } catch (IOException | RuntimeException e) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "메시지 형식이 올바르지 않습니다.");
            return null;
        }
    }

    private Long parseBidAmount(WebSocketSession session, Map<String, String> data) throws IOException {
        if (data == null) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 올바르지 않습니다.");
            return null;
        }
        String value = data.get("bidAmount");
        if (value == null || value.isBlank() || value.length() > 19) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 올바르지 않습니다.");
            return null;
        }
        try {
            long bidAmount = Long.parseLong(value);
            if (bidAmount <= 0) {
                sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 올바르지 않습니다.");
                return null;
            }
            return bidAmount;
        } catch (NumberFormatException e) {
            sendToOne(session, "error", HttpStatus.BAD_REQUEST, "입찰 금액이 올바르지 않습니다.");
            return null;
        }
    }

    private @NonNull TextMessage toTextMessage(@NonNull WebSocketResponseDTO response) throws IOException {
        String payload = Objects.requireNonNull(objectMapper.writeValueAsString(response));
        return Objects.requireNonNull(new TextMessage(payload));
    }

    // URI에서 경매 ID 추출
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

        if (exception instanceof IOException) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("세션 종료 중 오류 발생 : {}", e.getMessage());
            }
        }
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
