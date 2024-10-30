package com.inhatc.auction.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.common.constant.AuctionStatus;
import com.inhatc.auction.common.constant.TransactionStatus;
import com.inhatc.auction.config.jwt.JwtTokenProvider;
import com.inhatc.auction.domain.Auction;
import com.inhatc.auction.domain.Category;
import com.inhatc.auction.domain.Image;
import com.inhatc.auction.domain.Transaction;
import com.inhatc.auction.domain.User;
import com.inhatc.auction.dto.AuctionDetailResponseDTO;
import com.inhatc.auction.dto.AuctionRequestDTO;
import com.inhatc.auction.dto.AuctionResponseDTO;
import com.inhatc.auction.dto.BidResponseDTO;
import com.inhatc.auction.dto.ImageResponseDTO;
import com.inhatc.auction.repository.AuctionRepository;
import com.inhatc.auction.repository.BidRepository;
import com.inhatc.auction.repository.CategoryRepository;
import com.inhatc.auction.repository.TransactionRepository;
import com.inhatc.auction.repository.UserRepository;

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
  private final SseEmitterService sseEmitterService;
  private final JwtTokenProvider jwtTokenProvider;

  @Transactional
  public AuctionDetailResponseDTO getAuctionDetail(Long auctionId) {
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new RuntimeException("경매를 찾을 수 없습니다"));
    Long bidCount = this.bidRepository.findBidCountByAuctionId(auctionId).orElse(0L);
    Long watchCount = 0L;
    Long auctionLeftTime = this.auctionRepository.calculateAuctionLeftTime(auctionId) <= 0 ? 0L
        : this.auctionRepository.calculateAuctionLeftTime(auctionId);
    Long currentHighestBidUserId = this.bidRepository.findCurrentHighestBidUserId(auctionId).orElse(0L);
    String highestBidderNickname = this.userRepository.findNicknameById(currentHighestBidUserId);

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
            .bidTime(bid.getBidTime())
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
        .status(auction.getStatus())
        .highestBidderNickname(highestBidderNickname)
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
              (auction.getAuctionEndTime().toEpochSecond(ZoneOffset.ofHours(9))
                  - LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(9))));

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
              (auction.getAuctionEndTime().toEpochSecond(ZoneOffset.ofHours(9))
                  - LocalDateTime.now().toEpochSecond(ZoneOffset.ofHours(9))));

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
        .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

    Category category = this.categoryRepository.findById(categoryId)
        .orElseThrow(() -> new RuntimeException("카테고리를 찾을 수 없습니다"));

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
                throw new RuntimeException("이미지 파일 이름이 올바르지 않습니다");
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
                throw new RuntimeException(
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
              throw new RuntimeException("이미지 업로드 중 오류 발생", e);
            }
          })
          .collect(Collectors.toList());

      auction.setImages(imageList); // 마지막에 연결해줘야 auction_id가 image 테이블에 들어감
    } catch (Exception e) {
      log.error("이미지 업로드 중 오류 발생", e);
      throw new RuntimeException("이미지 업로드 중 오류 발생", e);
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
    auction.setAuctionEndTime(LocalDateTime.now());
    auction.setSuccessfulPrice(auction.getBuyNowPrice());
    auction.setStatus(AuctionStatus.ENDED);

    Transaction transaction = Transaction.builder()
        .auction(auction)
        .seller(auction.getUser())
        .buyer(user)
        .finalPrice(auction.getBuyNowPrice())
        .status(TransactionStatus.COMPLETED)
        .createdAt(LocalDateTime.now())
        .build();

    this.transactionRepository.save(transaction);

    this.auctionRepository.save(auction);

    // 경매 종료 알림
    sseEmitterService.broadcastAuctionEnded(auctionId);
  }

}
