package com.inhatc.auction.domain.auction.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auction.dto.request.AuctionRequestDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionDetailResponseDTO;
import com.inhatc.auction.domain.auction.dto.response.AuctionResponseDTO;
import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.auction.websocket.WebSocketHandler;
import com.inhatc.auction.domain.bid.dto.response.BidResponseDTO;
import com.inhatc.auction.domain.bid.repository.BidRepository;
import com.inhatc.auction.domain.category.entity.Category;
import com.inhatc.auction.domain.category.repository.CategoryRepository;
import com.inhatc.auction.domain.image.dto.response.ImageResponseDTO;
import com.inhatc.auction.domain.image.entity.Image;
import com.inhatc.auction.domain.sse.dto.response.SseTransactionResponseDTO;
import com.inhatc.auction.domain.sse.service.SseService;
import com.inhatc.auction.domain.transaction.dto.response.TransactionResponseDTO;
import com.inhatc.auction.domain.transaction.entity.Transaction;
import com.inhatc.auction.domain.transaction.repository.TransactionRepository;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.constant.AuctionStatus;
import com.inhatc.auction.global.constant.TransactionStatus;
import com.inhatc.auction.global.security.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
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
  private final BidRepository bidRepository;
  private final TransactionRepository transactionRepository;
  private final SseService sseEmitterService;
  private final JwtTokenProvider jwtTokenProvider;
  private final WebSocketHandler webSocketHandler;

  @Transactional
  public AuctionDetailResponseDTO getAuctionDetail(Long auctionId) {
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "경매를 찾을 수 없습니다"));
    Long bidCount = this.bidRepository.findBidCountByAuctionId(auctionId).orElse(0L);
    Long watchCount = 0L;
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

    List<ImageResponseDTO> imageList = auction.getImages().stream()
        .map(image -> ImageResponseDTO.builder()
            .filePath(image.getFilePath())
            .fileName(image.getFileName())
            .build())
        .collect(Collectors.toList());

    List<BidResponseDTO> bidList = auction.getBids().stream()
        .map(bid -> BidResponseDTO.builder()
            .id(bid.getId())
            .userId(bid.getUser().getId())
            .nickname(bid.getUser().getNickname())
            .bidAmount(bid.getBidAmount())
            .createdAt(bid.getCreatedAt())
            .build())
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
        .watchCount(watchCount)
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

  @Transactional
  public List<AuctionResponseDTO> getFeaturedAuctionList() {
    // 경매 입찰 수 내림차순으로 조회
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

  @Transactional
  public List<AuctionResponseDTO> getAuctionList() {
    // 현재시간보다 경매 종료 시간 + 10분 이후인 경매 조회
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

    User user = this.userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "사용자를 찾을 수 없습니다"));

    Category category = this.categoryRepository.findById(categoryId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
            "카테고리를 찾을 수 없습니다"));

    Auction auction = Auction.builder()
        .user(user)
        .category(category)
        .title(requestDTO.getTitle())
        .description(requestDTO.getDescription())
        .startPrice(requestDTO.getStartPrice())
        .buyNowPrice(requestDTO.getBuyNowPrice())
        .auctionStartTime(LocalDateTime.now())
        .auctionEndTime(LocalDateTime.now().plusDays(requestDTO.getAuctionDuration()))
        .status(AuctionStatus.ACTIVE)
        .build();

    try {
      List<Image> imageList = multipartFiles.stream()
          .map(image -> {
            try {
              if (image.getOriginalFilename() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "이미지 파일 타입이 올바르지 않습니다. 허용되는 타입: jpeg, png, jpg, gif, bmp, webp");
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
              throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                  "이미지 업로드 중 오류 발생", e);
            }
          })
          .collect(Collectors.toList());

      auction.setImages(imageList); // 마지막에 연결해줘야 auction_id가 image 테이블에 들어감
    } catch (Exception e) {
      log.error("이미지 업로드 중 오류 발생", e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지 업로드 중 오류 발생", e);
    }

    this.auctionRepository.save(auction);

    return auction.getId();
  }

  @Transactional
  public void buyNowAuction(HttpServletRequest request, Long auctionId) {
    // Authorization 헤더가 없는 경우 (로그인 안된 경우)
    if (request.getHeader("Authorization") == null)
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");

    // 토큰이 유효하지 않은 경우
    String accessToken = jwtTokenProvider.getTokenFromRequest(request);
    if (accessToken == null || !jwtTokenProvider.validateToken(accessToken)) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다. 다시 로그인해주세요.");
    }

    // 토큰에서 사용자 ID 추출
    Long userId = jwtTokenProvider.getUserIdFromToken(accessToken);

    // 사용자 조회
    User user = this.userRepository.findById(userId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다"));

    // 경매 조회
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));

    // 현재 경매가 종료된 경우
    if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 경매에는 즉시 구매할 수 없습니다.");
    }

    // 즉시 구매하려는 경매가 내가 등록한 경매인 경우
    if (auction.getUser().getId() == user.getId()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내가 등록한 경매에는 즉시 구매할 수 없습니다.");
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
    SseTransactionResponseDTO sseBuyNowResponseDTO = SseTransactionResponseDTO.builder()
        .userId(user.getId())
        .nickname(user.getNickname())
        .status(TransactionStatus.COMPLETED)
        .build();

    this.sseEmitterService.broadcastBuyNow(auctionId, sseBuyNowResponseDTO);
  }

  // 매 분마다 종료된 경매 업데이트
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void updateEndedAuctions() {
    LocalDateTime now = LocalDateTime.now();
    List<Auction> endedAuctions = auctionRepository.findByAuctionEndTimeBeforeAndStatus(now,
        AuctionStatus.ACTIVE);

    for (Auction auction : endedAuctions) {
      auction.updateStatus(AuctionStatus.ENDED);
      auction.setSuccessfulPrice(auction.getCurrentPrice());

      // 최종 거래 내역 저장
      Transaction transaction = Transaction.builder()
          .auction(auction)
          .seller(auction.getUser()) // 판매자
          .buyer(auction.getBids().get(auction.getBids().size() - 1).getUser()) // 최종 낙찰자
          .finalPrice(auction.getSuccessfulPrice())
          .status(TransactionStatus.COMPLETED)
          .build();

      this.transactionRepository.save(transaction);
      this.auctionRepository.save(auction);
      log.info("경매 ID: {} 종료됨", auction.getId());

      // 해당 경매를 조회하고있는 사용자들에게 경매 종료 알림 (WebSocket)
      this.webSocketHandler.broadcastEnded(auction);
    }
  }
}