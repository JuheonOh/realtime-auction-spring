package com.inhatc.auction.domain.auction.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.inhatc.auction.domain.auction.dto.request.AuctionRequestDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionDetailResponseDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionResponseDTO;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.dto.response.BidResponseDTO;
import com.inhatc.auction.domain.bid.entity.Bid;
import com.inhatc.auction.domain.bid.repository.BidRepository;
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
import com.inhatc.auction.domain.transaction.dto.response.TransactionResponseDTO;
import com.inhatc.auction.domain.transaction.entity.Transaction;
import com.inhatc.auction.domain.transaction.entity.TransactionStatus;
import com.inhatc.auction.domain.transaction.repository.TransactionRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.jwt.JwtTokenProvider;
import com.inhatc.auction.global.outbox.service.OutboxPublisher;
import com.inhatc.auction.global.utils.TimeUtils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuctionService {

  private static final int MAX_IMAGE_COUNT = 5;
  private static final long MAX_IMAGE_SIZE_BYTES = 10L * 1024 * 1024;
  private static final long MAX_IMAGE_PIXELS = 25_000_000L;
  private static final Map<String, ImageFormat> IMAGE_FORMATS = createImageFormats();

  @Value("${upload.path}")
  private String uploadPath;

  private final UserRepository userRepository;
  private final AuctionRepository auctionRepository;
  private final CategoryRepository categoryRepository;
  private final FavoriteRepository favoriteRepository;
  private final TransactionRepository transactionRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final BidRepository bidRepository;
  private final NotificationRepository notificationRepository;
  private final OutboxPublisher outboxPublisher;

  @Transactional(readOnly = true)
  public AuctionDetailResponseDTO getAuctionDetail(@NonNull HttpServletRequest request, @NonNull Long auctionId) {
    // 경매 조회
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다."));

    List<Bid> bids = bidRepository.findByAuctionId(auctionId);
    // 입찰 개수
    Long bidCount = (long) bids.size();

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

    List<BidResponseDTO> bidList = bids.stream()
        .sorted(Comparator.comparing(Bid::getBidTime))
        .map(bid -> {
          return BidResponseDTO.builder()
              .userId(bid.getUser().getId())
              .nickname(bid.getUser().getNickname())
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
  public Long createAuction(@NonNull Long userId, AuctionRequestDTO requestDTO) {
    Long categoryId = requestDTO.getCategoryId();
    List<MultipartFile> multipartFiles = requestDTO.getImages();

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

    validateImages(multipartFiles);
    Path uploadRoot = getUploadRoot();
    List<Path> savedFiles = new ArrayList<>();
    try {
      List<Image> imageList = new ArrayList<>();
      for (MultipartFile multipartFile : multipartFiles) {
        ImageFormat format = detectImageFormat(multipartFile);
        String fileSaveName = UUID.randomUUID() + "." + format.extension();
        Path target = uploadRoot.resolve(fileSaveName).normalize();
        if (!target.startsWith(uploadRoot)) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 저장 경로가 올바르지 않습니다.");
        }

        copyImageToTarget(multipartFile, uploadRoot, target);
        savedFiles.add(target);
        imageList.add(Image.builder()
            .filePath(fileSaveName)
            .fileName(multipartFile.getOriginalFilename())
            .fileType(format.contentType())
            .fileSize(multipartFile.getSize())
            .auction(auction)
            .build());
      }

      auction.setImages(imageList);
      this.auctionRepository.save(auction);
      if (TransactionSynchronizationManager.isSynchronizationActive()) {
        List<Path> filesToKeepOnCommit = List.copyOf(savedFiles);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status != TransactionSynchronization.STATUS_COMMITTED) {
              deleteFiles(filesToKeepOnCommit);
            }
          }
        });
      }
      return auction.getId();
    } catch (ResponseStatusException e) {
      deleteFiles(savedFiles);
      throw e;
    } catch (IOException | IllegalStateException e) {
      deleteFiles(savedFiles);
      log.error("이미지 업로드 중 오류 발생", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 중 오류 발생", e);
    } catch (RuntimeException e) {
      deleteFiles(savedFiles);
      throw e;
    }
  }

  private Path getUploadRoot() {
    try {
      Path configuredRoot = Paths.get(uploadPath).toAbsolutePath().normalize();
      Files.createDirectories(configuredRoot);
      return configuredRoot.toRealPath();
    } catch (IOException | IllegalArgumentException e) {
      log.error("이미지 업로드 경로를 준비하지 못했습니다", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 경로를 준비하지 못했습니다.", e);
    }
  }

  private void validateImages(List<MultipartFile> images) {
    if (images == null || images.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지를 최소 1개 이상 업로드해야 합니다.");
    }
    if (images.size() > MAX_IMAGE_COUNT) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지는 최대 " + MAX_IMAGE_COUNT + "개까지 업로드할 수 있습니다.");
    }
    for (MultipartFile image : images) {
      if (image == null || image.isEmpty() || image.getSize() <= 0
          || image.getOriginalFilename() == null || image.getOriginalFilename().isBlank()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 올바르지 않습니다.");
      }
      if (image.getSize() > MAX_IMAGE_SIZE_BYTES) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일은 10MB 이하여야 합니다.");
      }
    }
  }

  private ImageFormat detectImageFormat(MultipartFile image) throws IOException {
    try (ImageInputStream input = ImageIO.createImageInputStream(image.getInputStream())) {
      if (input == null) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 올바르지 않습니다.");
      }
      java.util.Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일 타입이 올바르지 않습니다. 허용 타입: jpeg, png, gif, bmp, webp");
      }

      ImageReader reader = readers.next();
      try {
        reader.setInput(input, true, true);
        ImageFormat format = IMAGE_FORMATS.get(reader.getFormatName().toLowerCase(Locale.ROOT));
        if (format == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일 타입이 올바르지 않습니다. 허용 타입: jpeg, png, gif, bmp, webp");
        }
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 해상도가 허용 범위를 초과했습니다.");
        }
        BufferedImage decodedImage = reader.read(0);
        if (decodedImage == null) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일이 올바르지 않습니다.");
        }
        return format;
      } finally {
        reader.dispose();
      }
    }
  }

  private void copyImageToTarget(MultipartFile image, Path uploadRoot, Path target) throws IOException {
    Path temporaryFile = Files.createTempFile(uploadRoot, ".upload-", ".tmp");
    try {
      try (java.io.InputStream input = image.getInputStream()) {
        Files.copy(input, temporaryFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
      }
      try {
        Files.move(temporaryFile, target, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
      } catch (java.nio.file.AtomicMoveNotSupportedException e) {
        Files.move(temporaryFile, target);
      }
    } finally {
      Files.deleteIfExists(temporaryFile);
    }
  }

  private void deleteFiles(List<Path> files) {
    for (Path file : files) {
      try {
        Files.deleteIfExists(file);
      } catch (IOException e) {
        log.warn("실패한 이미지 업로드 파일을 삭제하지 못했습니다: {}", file, e);
      }
    }
  }

  private static Map<String, ImageFormat> createImageFormats() {
    Map<String, ImageFormat> formats = new HashMap<>();
    formats.put("jpeg", new ImageFormat("jpg", "image/jpeg"));
    formats.put("jpg", new ImageFormat("jpg", "image/jpeg"));
    formats.put("png", new ImageFormat("png", "image/png"));
    formats.put("gif", new ImageFormat("gif", "image/gif"));
    formats.put("bmp", new ImageFormat("bmp", "image/bmp"));
    formats.put("webp", new ImageFormat("webp", "image/webp"));
    return Map.copyOf(formats);
  }

  private record ImageFormat(String extension, String contentType) {
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

    Auction auction = this.auctionRepository.findByIdForUpdate(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));

    Transaction existingTransaction = transactionRepository.findByAuctionId(auctionId).orElse(null);
    if (existingTransaction != null) {
      auction.setSuccessfulPrice(existingTransaction.getFinalPrice());
      auction.updateStatus(AuctionStatus.ENDED);
      auctionRepository.save(auction);
      return;
    }

    if (auction.getBuyNowPrice() == 0) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "즉시 구매가 가능한 경매가 아닙니다.");
    }

    if (auction.getStatus() != AuctionStatus.ACTIVE) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미 종료된 경매입니다.");
    }

    if (!auction.getAuctionEndTime().isAfter(LocalDateTime.now())) {
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
    outboxPublisher.publishWebSocket(auction.getId(), "buy-now", HttpStatus.CREATED.value(),
        JsonNodeFactory.instance.objectNode().put("buyerId", user.getId()));
  }

  // 즉시구매 종료 시: 구매자는 BUY_NOW_WIN, 기존 입찰자는 ENDED 알림 전송
  private void notifyAuctionEndedToBidders(Auction auction, User buyer) {
    List<Bid> auctionBidList = bidRepository.findByAuctionId(auction.getId());
    List<Long> bidderIds = auctionBidList
        .stream()
        .map(bid -> bid.getUser().getId())
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

      outboxPublisher.publishSse(recipientId, notificationResponseDTO);
    }
  }

  // 마감 시간 도달 시 패찰자(비낙찰자) 알림 전송
  private void notifyAuctionEndedByTimeToLosers(Auction auction, Long winnerId, List<Bid> bidList) {
    List<Long> loserIds = bidList.stream()
        .map(bid -> bid.getUser().getId())
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

      outboxPublisher.publishSse(loserId, notificationResponseDTO);
    }
  }

  // 특정 사용자의 최고 입찰가 정보 반환
  private MyBidInfoDTO getHighestMyBidInfo(List<Bid> bidList, Long userId) {
    if (bidList == null || userId == null) {
      return null;
    }

    return bidList.stream()
        .filter(bid -> userId.equals(bid.getUser().getId()))
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

    for (Auction endedAuction : endedAuctions) {
      Auction auction = auctionRepository.findByIdForUpdate(endedAuction.getId()).orElse(null);
      if (auction == null || auction.getStatus() != AuctionStatus.ACTIVE
          || auction.getAuctionEndTime().isAfter(LocalDateTime.now())) {
        continue;
      }
      Transaction existingTransaction = transactionRepository.findByAuctionId(auction.getId()).orElse(null);
      if (existingTransaction != null) {
        auction.setSuccessfulPrice(existingTransaction.getFinalPrice());
        auction.updateStatus(AuctionStatus.ENDED);
        auctionRepository.save(auction);
        continue;
      }

      List<Bid> bidList = bidRepository.findByAuctionId(auction.getId());
      // 최고 입찰자
      Bid highestBid = bidList.isEmpty() ? null : bidList.get(0);

      // 입찰 내역이 없는 경우
      if (highestBid == null) {
        auction.updateStatus(AuctionStatus.ENDED);
        auction.setSuccessfulPrice(0L);
        this.auctionRepository.save(auction);
        outboxPublisher.publishWebSocket(auction.getId(), "ended", HttpStatus.OK.value(),
            JsonNodeFactory.instance.objectNode());
      } else {
        // 입찰자가 있는 경우
        Long highestBidUserId = highestBid.getUser().getId();
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

          Long winnerId = winner.get().getId();
          if (winnerId == null) {
            log.warn("Auction ID: {} has winner without userId", auction.getId());
            continue;
          }
          outboxPublisher.publishSse(winnerId, notificationResponseDTO);

          notifyAuctionEndedByTimeToLosers(auction, winnerId, bidList);
          outboxPublisher.publishWebSocket(auction.getId(), "ended", HttpStatus.OK.value(),
              JsonNodeFactory.instance.objectNode());
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

}
