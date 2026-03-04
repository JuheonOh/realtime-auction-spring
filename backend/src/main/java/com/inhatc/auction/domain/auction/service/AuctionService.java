package com.inhatc.auction.domain.auction.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auction.dto.request.AuctionRequestDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionDetailResponseDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionResponseDTO;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.dto.response.BidResponseDTO;
import com.inhatc.auction.domain.bid.entity.RedisBid;
import com.inhatc.auction.domain.bid.repository.RedisBidRepository;
import com.inhatc.auction.domain.bid.websocket.WebSocketHandler;
import com.inhatc.auction.domain.category.entity.Category;
import com.inhatc.auction.domain.category.repository.CategoryRepository;
import com.inhatc.auction.domain.favorite.entity.Favorite;
import com.inhatc.auction.domain.favorite.repository.FavoriteRepository;
import com.inhatc.auction.domain.image.dto.response.ImageResponseDTO;
import com.inhatc.auction.domain.image.entity.Image;
import com.inhatc.auction.domain.notification.dto.response.AuctionInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.MyBidInfoDTO;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.repository.NotificationRepository;
import com.inhatc.auction.domain.notification.service.SseNotificationService;
import com.inhatc.auction.domain.transaction.dto.response.TransactionResponseDTO;
import com.inhatc.auction.domain.transaction.entity.Transaction;
import com.inhatc.auction.domain.transaction.entity.TransactionStatus;
import com.inhatc.auction.domain.transaction.repository.TransactionRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.jwt.JwtTokenProvider;
import com.inhatc.auction.global.utils.TimeUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuctionService {

  @Value("${upload.path}")
  private String uploadPath;

  private final UserRepository userRepository;
  private final AuctionRepository auctionRepository;
  private final CategoryRepository categoryRepository;
  private final FavoriteRepository favoriteRepository;
  private final TransactionRepository transactionRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final RedisBidRepository redisBidRepository;
  private final NotificationRepository notificationRepository;
  private final SseNotificationService sseNotificationService;
  private final WebSocketHandler webSocketHandler;

  @Transactional(readOnly = true)
  public AuctionDetailResponseDTO getAuctionDetail(@NonNull HttpServletRequest request, @NonNull Long auctionId) {
    // 경매 조회
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

    // Redis 입찰 내역 조회
    List<RedisBid> redisBids = redisBidRepository.findByAuctionIdOrderByBidTimeAsc(auctionId);
    // 입찰 개수
    Long bidCount = (long) redisBids.size();

    // 기본 관심 상태는 false
    boolean isFavorite = false;
    // 로그인된 경우 관심 경매 여부 확인
    if (request.getHeader("Authorization") != null) {
      String accessToken = jwtTokenProvider.getTokenFromRequest(request);
      if (accessToken != null) {
        Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));
        isFavorite = favoriteRepository.existsByUserAndAuction(user, auction);
      }
    }

    // 관심 개수
    Long favoriteCount = this.favoriteRepository.countByAuctionId(auctionId);

    // 경매 종료까지 남은 시간(초)
    Long auctionLeftTime = Math.max(0,
        Duration.between(LocalDateTime.now(), auction.getAuctionEndTime()).toSeconds());

    // 경매 낙찰 내역 조회
    Transaction transaction = this.transactionRepository.findByAuctionId(auctionId).orElse(null);
    TransactionResponseDTO transactionResponseDTO = null;
    if (transaction != null) {
      transactionResponseDTO = TransactionResponseDTO.builder()
          .userId(transaction.getBuyer().getId())
          .nickname(transaction.getBuyer().getNickname())
          .status(transaction.getStatus())
          .finalPrice(transaction.getFinalPrice())
          .build();
    }

    // 경매 이미지 리스트
    List<ImageResponseDTO> imageList = auction.getImages().stream()
        .map(image -> ImageResponseDTO.builder()
            .filePath(image.getFilePath())
            .fileName(image.getFileName())
            .build())
        .collect(Collectors.toList());

    // Redis 입찰 내역을 DTO로 변환
    List<BidResponseDTO> bidList = redisBids.stream()
        .map((RedisBid bid) -> {
          Long bidderId = bid.getUserId();
          if (bidderId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입찰자 ID가 누락되었습니다.");
          }
          User bidder = userRepository.findById(bidderId)
              .orElseThrow(() -> new ResponseStatusException(
                  HttpStatus.NOT_FOUND,
                  "입찰자 정보를 찾을 수 없습니다."));

          return BidResponseDTO.builder()
              .userId(bidderId)
              .nickname(bidder.getNickname())
              .bidAmount(bid.getBidAmount())
              .bidTime(bid.getBidTime())
              .build();
        })
        .collect(Collectors.toList());

    return AuctionDetailResponseDTO.builder()
        .id(auction.getId())
        .userId(auction.getUser().getId())
        .nickname(auction.getUser().getNickname())
        .categoryName(auction.getCategory().getName())
        .title(auction.getTitle())
        .description(auction.getDescription())
        .startPrice(auction.getStartPrice())
        .currentPrice(auction.getCurrentPrice())
        .buyNowPrice(auction.getBuyNowPrice())
        .bidCount(bidCount)
        .favoriteCount(favoriteCount)
        .isFavorite(isFavorite)
        .auctionStartTime(auction.getAuctionStartTime())
        .auctionEndTime(auction.getAuctionEndTime())
        .auctionLeftTime(auctionLeftTime)
        .successfulPrice(auction.getSuccessfulPrice())
        .status(auction.getStatus())
        .transaction(transactionResponseDTO)
        .images(imageList)
        .bids(bidList)
        .createdAt(auction.getCreatedAt())
        .updatedAt(auction.getUpdatedAt())
        .build();
  }

  @Transactional(readOnly = true)
  public List<AuctionResponseDTO> getFeaturedAuctionList() {
    // 입찰 수 내림차순으로 조회
    List<Auction> auctions = this.auctionRepository.findAllByOrderByBidCountDesc();

    return auctions.stream()
        .map(auction -> {
          Long auctionLeftTime = Math.max(0,
              Duration.between(LocalDateTime.now(),
                  auction.getAuctionEndTime()).toSeconds());

          return AuctionResponseDTO.builder()
              .id(auction.getId())
              .userId(auction.getUser().getId())
              .nickname(auction.getUser().getNickname())
              .categoryName(auction.getCategory().getName())
              .title(auction.getTitle())
              .image(auction.getImages().get(0).getFilePath())
              .currentPrice(auction.getCurrentPrice())
              .buyNowPrice(auction.getBuyNowPrice())
              .auctionStartTime(auction.getAuctionStartTime())
              .auctionEndTime(auction.getAuctionEndTime())
              .auctionLeftTime(auctionLeftTime)
              .createdAt(auction.getCreatedAt())
              .updatedAt(auction.getUpdatedAt())
              .build();
        })
        .collect(Collectors.toList());
  }

  @Transactional(readOnly = true)
  public List<AuctionResponseDTO> getAuctionList() {
    // 현재 시간 기준 종료 10분 이후 경매 조회
    List<Auction> auctions = auctionRepository.findAllByAuctionEndTimeAfter(10);

    return auctions.stream()
        .map(auction -> {
          String imagePath = auction.getImages().get(0).getFilePath();

          Long auctionLeftTime = Math.max(0,
              (auction.getAuctionEndTime()
                  .toEpochSecond(ZoneOffset.ofHours(9))
                  - LocalDateTime.now().toEpochSecond(
                      ZoneOffset.ofHours(9))));

          return AuctionResponseDTO.builder()
              .id(auction.getId())
              .userId(auction.getUser().getId())
              .nickname(auction.getUser().getNickname())
              .categoryName(auction.getCategory().getName())
              .title(auction.getTitle())
              .image(imagePath)
              .currentPrice(auction.getCurrentPrice())
              .buyNowPrice(auction.getBuyNowPrice())
              .auctionStartTime(auction.getAuctionStartTime())
              .auctionEndTime(auction.getAuctionEndTime())
              .auctionLeftTime(auctionLeftTime)
              .createdAt(auction.getCreatedAt())
              .updatedAt(auction.getUpdatedAt())
              .build();
        })
        .collect(Collectors.toList());
  }

  @Transactional
  public Long createAuction(AuctionRequestDTO requestDTO) {
    Long userId = requestDTO.getUserId();
    Long categoryId = requestDTO.getCategoryId();
    List<MultipartFile> multipartFiles = requestDTO.getImages();

    if (userId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId는 필수값입니다");
    }
    if (categoryId == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "categoryId는 필수값입니다");
    }

    User user = this.userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

    Category category = this.categoryRepository.findById(categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "카테고리를 찾을 수 없습니다"));

    Auction auction = Auction.builder()
        .user(user)
        .category(category)
        .title(requestDTO.getTitle())
        .description(requestDTO.getDescription())
        .startPrice(requestDTO.getStartPrice())
        .buyNowPrice(requestDTO.getBuyNowPrice())
        .currentPrice(requestDTO.getStartPrice())
        .auctionStartTime(LocalDateTime.now())
        .auctionEndTime(LocalDateTime.now().plusDays(requestDTO.getAuctionDuration()))
        .status(AuctionStatus.ACTIVE)
        .build();

    try {
      List<Image> imageList = multipartFiles.stream()
          .map(image -> {
            try {
              if (image.getOriginalFilename() == null) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지 파일 이름이 올바르지 않습니다");
              }

              String fileSaveName = UUID.randomUUID().toString() + "_"
                  + image.getOriginalFilename();
              String fileRealName = image.getOriginalFilename();
              String fileType = image.getContentType();
              long fileSize = image.getSize();
              Path uploadDir = Paths.get(uploadPath);

              if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
              }

              if (fileType != null && !fileType.equals("image/jpeg")
                  && !fileType.equals("image/png")
                  && !fileType.equals("image/jpg")
                  && !fileType.equals("image/gif")
                  && !fileType.equals("image/bmp")
                  && !fileType.equals("image/webp")) {
                throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "이미지 파일 타입이 올바르지 않습니다. 허용 타입: jpeg, png, jpg, gif, bmp, webp");
              }

              Path filePath = uploadDir.resolve(fileSaveName);
              Files.copy(image.getInputStream(), filePath);

              return Image.builder()
                  .filePath(fileSaveName)
                  .fileName(fileRealName)
                  .fileType(fileType)
                  .fileSize(fileSize)
                  .auction(auction)
                  .build();
            } catch (IllegalStateException | IOException e) {
              log.error("이미지 업로드 중 오류 발생", e);
              throw new ResponseStatusException(
                  HttpStatus.INTERNAL_SERVER_ERROR,
                  "이미지 업로드 중 오류 발생", e);
            }
          })
          .collect(Collectors.toList());

      auction.setImages(imageList);
    } catch (Exception e) {
      log.error("이미지 업로드 중 오류 발생", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 중 오류 발생", e);
    }

    this.auctionRepository.save(auction);

    return auction.getId();
  }

  @Transactional
  public void buyNowAuction(@NonNull HttpServletRequest request, @NonNull Long auctionId) {
    // Authorization 헤더가 없는 경우
    if (request.getHeader("Authorization") == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
    }

    // 토큰 유효성 검증
    String accessToken = jwtTokenProvider.getTokenFromRequest(request);
    if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해 주세요");
    }

    // 토큰에서 사용자 ID 추출
    Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

    // 사용자/경매 조회
    User user = this.userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));

    if (auction.getBuyNowPrice() == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "즉시 구매가 가능한 경매가 아닙니다.");
    }

    if (auction.getStatus() == AuctionStatus.ENDED) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다.");
    }

    if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 경매는 즉시 구매할 수 없습니다.");
    }

    if (auction.getUser().getId().equals(user.getId())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "본인이 등록한 경매는 즉시 구매할 수 없습니다.");
    }

    // 경매 상태 및 종료 시간 업데이트
    auction.updateAuctionEndTime(LocalDateTime.now());
    auction.setSuccessfulPrice(auction.getBuyNowPrice());
    auction.updateStatus(AuctionStatus.ENDED);

    // 최종 거래 저장
    Transaction transaction = Transaction.builder()
        .auction(auction)
        .seller(auction.getUser())
        .buyer(user)
        .finalPrice(auction.getBuyNowPrice())
        .status(TransactionStatus.COMPLETED)
        .build();
    this.transactionRepository.save(Objects.requireNonNull(transaction));

    this.auctionRepository.save(auction);

    // 구매자는 BUY_NOW_WIN, 기존 입찰자는 ENDED 알림 전송
    notifyAuctionEndedToBidders(auction, user);
    // 경매방 참여자에게 즉시구매 결과 브로드캐스트
    this.webSocketHandler.broadcastBuyNow(auction, user);
  }

  // 즉시구매 종료 시: 구매자는 BUY_NOW_WIN, 기존 입찰자는 ENDED 알림 전송
  private void notifyAuctionEndedToBidders(Auction auction, User buyer) {
    // 경매의 입찰 참여자 목록 조회
    List<RedisBid> auctionBidList = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId());
    List<Long> bidderIds = auctionBidList
        .stream()
        .map(RedisBid::getUserId)
        .distinct()
        .collect(Collectors.toList());

    Set<Long> recipientIds = new LinkedHashSet<>(bidderIds);
    recipientIds.add(buyer.getId());

    // 알림 payload에 사용할 대표 이미지 정보
    String filePath = null;
    String fileName = null;
    if (auction.getImages() != null && !auction.getImages().isEmpty()) {
      filePath = auction.getImages().get(0).getFilePath();
      fileName = auction.getImages().get(0).getFileName();
    }

    for (Long recipientId : recipientIds) {
      if (recipientId == null) {
        continue;
      }
      User recipient = userRepository.findById(recipientId).orElse(null);
      if (recipient == null) {
        continue;
      }

      NotificationType notificationType = recipientId.equals(buyer.getId())
          ? NotificationType.BUY_NOW_WIN
          : NotificationType.ENDED;

      // 동일 타입 중복 알림 soft-delete
      notificationRepository.findDuplicatedNotification(recipientId, notificationType, auction.getId())
          .ifPresent(duplicate -> {
            duplicate.markAsDeleted();
            notificationRepository.save(duplicate);
          });

      // 알림 생성/저장
      Notification endedNotification = Notification.builder()
          .user(recipient)
          .type(notificationType)
          .auctionId(auction.getId())
          .build();

      notificationRepository.save(Objects.requireNonNull(endedNotification));

      NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
          .id(endedNotification.getId())
          .type(endedNotification.getType())
          .isRead(endedNotification.getIsRead())
          .time(TimeUtils.getRelativeTimeString(endedNotification.getCreatedAt()))
          .auctionInfo(AuctionInfoDTO.builder()
              .id(auction.getId())
              .title(auction.getTitle())
              .currentPrice(auction.getCurrentPrice())
              .successfulPrice(auction.getSuccessfulPrice())
              .filePath(filePath)
              .fileName(fileName)
              .auctionEndTime(auction.getAuctionEndTime())
              .build())
          .myBidInfo(notificationType == NotificationType.ENDED
              ? getHighestMyBidInfo(auctionBidList, recipientId)
              : null)
          .build();

      // SSE 푸시 전송
      sendNotificationSafely(recipientId, notificationResponseDTO);
    }
  }

  // 마감 시간 도달 시 패찰자(비낙찰자) 알림 전송
  private void notifyAuctionEndedByTimeToLosers(Auction auction, Long winnerId, List<RedisBid> bidList) {
    List<Long> loserIds = bidList.stream()
        .map(RedisBid::getUserId)
        .distinct()
        .filter(userId -> !userId.equals(winnerId))
        .collect(Collectors.toList());

    if (loserIds.isEmpty()) {
      return;
    }

    String filePath = null;
    String fileName = null;
    if (auction.getImages() != null && !auction.getImages().isEmpty()) {
      filePath = auction.getImages().get(0).getFilePath();
      fileName = auction.getImages().get(0).getFileName();
    }

    for (Long loserId : loserIds) {
      if (loserId == null) {
        continue;
      }
      User loser = userRepository.findById(loserId).orElse(null);
      if (loser == null) {
        continue;
      }

      notificationRepository.findDuplicatedNotification(loserId, NotificationType.ENDED_TIME, auction.getId())
          .ifPresent(duplicate -> {
            duplicate.markAsDeleted();
            notificationRepository.save(duplicate);
          });

      Notification endedTimeNotification = Notification.builder()
          .user(loser)
          .type(NotificationType.ENDED_TIME)
          .auctionId(auction.getId())
          .build();

      notificationRepository.save(Objects.requireNonNull(endedTimeNotification));

      NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
          .id(endedTimeNotification.getId())
          .type(endedTimeNotification.getType())
          .isRead(endedTimeNotification.getIsRead())
          .time(TimeUtils.getRelativeTimeString(endedTimeNotification.getCreatedAt()))
          .auctionInfo(AuctionInfoDTO.builder()
              .id(auction.getId())
              .title(auction.getTitle())
              .currentPrice(auction.getCurrentPrice())
              .successfulPrice(auction.getSuccessfulPrice())
              .filePath(filePath)
              .fileName(fileName)
              .auctionEndTime(auction.getAuctionEndTime())
              .build())
          .myBidInfo(getHighestMyBidInfo(bidList, loserId))
          .build();

      sendNotificationSafely(loserId, notificationResponseDTO);
    }
  }

  // 특정 사용자의 최고 입찰가 정보 반환
  private MyBidInfoDTO getHighestMyBidInfo(List<RedisBid> bidList, Long userId) {
    if (bidList == null || userId == null) {
      return null;
    }

    return bidList.stream()
        .filter(bid -> userId.equals(bid.getUserId()))
        .findFirst()
        .map(bid -> MyBidInfoDTO.builder()
            .bidAmount(bid.getBidAmount())
            .build())
        .orElse(null);
  }

  // 30초마다 종료된 경매 정산
  @Scheduled(fixedRate = 30000)
  @Transactional
  public void updateEndedAuctions() {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> endedAuctions = auctionRepository.findByAuctionEndTimeBeforeAndStatus(now, AuctionStatus.ACTIVE);

    for (Auction auction : endedAuctions) {
      // Redis에서 금액순으로 정렬된 입찰 내역 조회
      List<RedisBid> bidList = redisBidRepository.findByAuctionIdOrderByBidAmountDesc(auction.getId());
      // 최고 입찰자
      RedisBid highestBid = bidList.isEmpty() ? null : bidList.get(0);

      // 입찰 내역이 없는 경우
      if (highestBid == null) {
        auction.updateStatus(AuctionStatus.ENDED);
        auction.setSuccessfulPrice(0L);
        this.auctionRepository.save(auction);
      } else {
        // 입찰자가 있는 경우
        Long highestBidUserId = highestBid.getUserId();
        if (highestBidUserId == null) {
          log.warn("Auction ID: {} has highest bid without userId", auction.getId());
          continue;
        }
        Optional<User> winner = userRepository.findById(highestBidUserId);

        if (winner.isPresent()) {
          auction.updateStatus(AuctionStatus.ENDED);
          auction.setSuccessfulPrice(highestBid.getBidAmount());

          Transaction transaction = Transaction.builder()
              .auction(auction)
              .seller(auction.getUser())
              .buyer(winner.get())
              .finalPrice(highestBid.getBidAmount())
              .status(TransactionStatus.COMPLETED)
              .build();

          this.transactionRepository.save(Objects.requireNonNull(transaction));
          this.auctionRepository.save(auction);
          log.info("Auction ID: {} ended", auction.getId());

          // 경매 낙찰 알림 생성
          Notification notification = Notification.builder()
              .user(winner.get())
              .type(NotificationType.WIN)
              .auctionId(auction.getId())
              .build();

          this.notificationRepository.save(Objects.requireNonNull(notification));

          NotificationResponseDTO notificationResponseDTO = NotificationResponseDTO.builder()
              .id(notification.getId())
              .type(notification.getType())
              .isRead(notification.getIsRead())
              .time(TimeUtils.getRelativeTimeString(notification.getCreatedAt()))
              .auctionInfo(AuctionInfoDTO.builder()
                  .id(auction.getId())
                  .title(auction.getTitle())
                  .successfulPrice(highestBid.getBidAmount())
                  .filePath(auction.getImages().get(0).getFilePath())
                  .fileName(auction.getImages().get(0).getFileName())
                  .auctionEndTime(auction.getAuctionEndTime())
                  .build())
              .build();

          // 낙찰자에게 SSE 알림 전송
          Long winnerId = winner.get().getId();
          if (winnerId == null) {
            log.warn("Auction ID: {} has winner without userId", auction.getId());
            continue;
          }
          sendNotificationSafely(winnerId, notificationResponseDTO);

          // 비낙찰자 ENDED_TIME 알림 및 WebSocket 종료 브로드캐스트
          notifyAuctionEndedByTimeToLosers(auction, winnerId, bidList);
          this.webSocketHandler.broadcastEnded(auction);
        }
      }
    }
  }

  // 관심 경매 등록/해제
  @Transactional
  public void favoriteAuction(@NonNull HttpServletRequest request, @NonNull Long auctionId) {
    String accessToken = jwtTokenProvider.getTokenFromRequest(request);
    if (accessToken == null) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
    }
    Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

    User user = this.userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));

    // 기존에 관심한 경매면 제거, 아니면 신규 등록
    boolean isFavorite = this.favoriteRepository.existsByUserAndAuction(user, auction);
    if (isFavorite) {
      this.favoriteRepository.deleteByUserAndAuction(user, auction);
    } else {
      Favorite favorite = Favorite.builder().user(user).auction(auction).build();
      this.favoriteRepository.save(Objects.requireNonNull(favorite));
    }
  }

  private void sendNotificationSafely(Long userId, NotificationResponseDTO dto) {
    sseNotificationService.sendNotification(
        Objects.requireNonNull(userId),
        Objects.requireNonNull(dto));
  }
}
