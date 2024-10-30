package com.inhatc.auction.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.config.jwt.JwtTokenProvider;
import com.inhatc.auction.domain.Auction;
import com.inhatc.auction.domain.Bid;
import com.inhatc.auction.domain.User;
import com.inhatc.auction.dto.BidRequestDTO;
import com.inhatc.auction.dto.BidResponseDTO;
import com.inhatc.auction.dto.SseBidResponseDTO;
import com.inhatc.auction.repository.AuctionRepository;
import com.inhatc.auction.repository.BidRepository;
import com.inhatc.auction.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
public class BidService {
  private final JwtTokenProvider jwtTokenProvider;
  private final UserRepository userRepository;
  private final BidRepository bidRepository;
  private final AuctionRepository auctionRepository;
  private final SseEmitterService sseEmitterService;

  public List<BidResponseDTO> getBidList(Long auctionId) {
    List<Bid> bids = this.bidRepository.findAllByAuctionId(auctionId);
    return bids.stream()
        .map(bid -> BidResponseDTO.builder()
            .id(bid.getId())
            .nickname(bid.getUser().getNickname())
            .bidAmount(bid.getBidAmount())
            .bidTime(bid.getBidTime())
            .build())
        .collect(Collectors.toList());
  }

  @Transactional
  public void createBid(HttpServletRequest request, Long auctionId, BidRequestDTO requestDTO) {
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

    // 입찰 금액
    Long bidAmount = requestDTO.getBidAmount();

    // 경매 조회
    Auction auction = this.auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "경매를 찾을 수 없습니다"));

    // 입찰 횟수 조회
    Boolean isFirstBid = this.bidRepository.findBidCountByAuctionId(auctionId).orElse(null) == 0;

    // 현재 경매가 종료된 경우
    if (auction.getAuctionEndTime().isBefore(LocalDateTime.now())) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료된 경매에는 입찰할 수 없습니다.");
    }

    // 입찰하려는 경매가 내가 등록한 경매인 경우
    if (auction.getUser().getId() == user.getId()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "내가 등록한 경매에는 입찰할 수 없습니다.");
    }

    // 현재 최고입찰자가 본인인 경우 입찰할 수 없음
    Long currentHighestBidUserId = this.bidRepository.findCurrentHighestBidUserId(auctionId).orElse(null);
    if (currentHighestBidUserId != null && currentHighestBidUserId == user.getId()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "현재 고객님이 최고입찰자입니다.");
    }

    // 첫 입찰의 경우 시작가와 같거나 높아야 함
    if (isFirstBid) {
      if (bidAmount < auction.getStartPrice()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입찰 금액이 시작가와 같거나 높아야 합니다");
      }
    } else {
      // 첫 입찰이 아닌 경우 현재 경매 가격보다 높아야 함
      if (bidAmount <= auction.getCurrentPrice()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "입찰 금액을 현재 경매 가격보다 높게 입력해주세요.");
      }
    }

    // 입찰 저장
    Bid bid = Bid.builder()
        .auction(auction)
        .user(user)
        .bidAmount(bidAmount)
        .bidTime(LocalDateTime.now())
        .build();
    this.bidRepository.save(bid);

    // 현재 경매 가격 업데이트
    auction.setCurrentPrice(bidAmount);
    this.auctionRepository.save(auction);

    // 경매 남은 시간
    Long auctionLeftTime = Math.max(0, Duration.between(LocalDateTime.now(), auction.getAuctionEndTime()).toSeconds());

    // 입찰 응답 DTO 생성
    SseBidResponseDTO sseBidResponseDTO = SseBidResponseDTO.builder()
        .id(bid.getId())
        .userId(bid.getUser().getId())
        .nickname(bid.getUser().getNickname())
        .bidAmount(bid.getBidAmount())
        .bidTime(bid.getBidTime())
        .auctionLeftTime(auctionLeftTime)
        .build();

    // 현재 경매에 조회중인 모든 사용자에게 입찰 데이터 전송
    sseEmitterService.broadcastBid(auctionId, sseBidResponseDTO);
  }

}
