package com.inhatc.auction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import com.inhatc.auction.domain.auction.entity.Auction;
import com.inhatc.auction.domain.auction.entity.AuctionStatus;
import com.inhatc.auction.domain.auction.repository.AuctionRepository;
import com.inhatc.auction.domain.bid.repository.RedisBidRepository;
import com.inhatc.auction.domain.category.entity.Category;
import com.inhatc.auction.domain.image.entity.Image;
import com.inhatc.auction.domain.notification.dto.response.NotificationResponseDTO;
import com.inhatc.auction.domain.notification.entity.Notification;
import com.inhatc.auction.domain.notification.entity.NotificationType;
import com.inhatc.auction.domain.notification.repository.NotificationRepository;
import com.inhatc.auction.domain.notification.service.NotificationService;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.entity.UserRole;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 스프링 부트 기본 컨텍스트 로딩 테스트.
 */
@SpringBootTest
class AuctionApplicationTests {

    /**
     * 애플리케이션 컨텍스트가 정상적으로 기동되는지 확인한다.
     */
    @Test
    void contextLoads() {
    }
}

/**
 * NotificationService의 구매자 알림 타입 분기(WIN/BUY_NOW_WIN)를 단위 테스트로 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class AuctionApplicationBuyerNotificationTypeTests {

    // NotificationService 의존성을 Mock으로 주입한다.
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private RedisBidRepository redisBidRepository;

    // 테스트 대상 서비스
    @InjectMocks
    private NotificationService notificationService;

    /**
     * WIN 알림 타입이 그대로 전달되고,
     * WIN 분기에서는 Redis 입찰 조회가 호출되지 않는지 검증한다.
     */
    @Test
    void getNotifications_whenBuyerTypeIsWin_shouldReturnWinNotification() {
        // given
        Long userId = 1L;
        Long auctionId = 101L;
        String accessToken = "access-token";

        HttpServletRequest request = mock(HttpServletRequest.class);
        Notification notification = createNotification(11L, auctionId, NotificationType.WIN);
        Auction auction = createAuctionWithImage(auctionId, 12000L);

        // 토큰 파싱 -> 알림 조회 -> 경매 조회 흐름 mock 설정
        when(jwtTokenProvider.getTokenFromRequest(Objects.requireNonNull(request))).thenReturn(accessToken);
        when(jwtTokenProvider.getUserIdFromToken(accessToken)).thenReturn(userId);
        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        // when
        List<NotificationResponseDTO> result = notificationService.getNotifications(request);

        // then
        assertThat(result).hasSize(1);
        NotificationResponseDTO response = result.stream().findFirst().orElseThrow();
        assertThat(response).isNotNull();
        assertThat(response.getAuctionInfo()).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.WIN);
        assertThat(response.getAuctionInfo().getSuccessfulPrice()).isEqualTo(12000L);
        assertThat(response.getMyBidInfo()).isNull();
        verify(redisBidRepository, never()).findByAuctionIdOrderByBidAmountDesc(auctionId);
    }

    /**
     * BUY_NOW_WIN 알림 타입이 그대로 전달되고,
     * BUY_NOW_WIN 분기에서도 Redis 입찰 조회가 호출되지 않는지 검증한다.
     */
    @Test
    void getNotifications_whenBuyerTypeIsBuyNowWin_shouldReturnBuyNowWinNotification() {
        // given
        Long userId = 1L;
        Long auctionId = 202L;
        String accessToken = "access-token";

        HttpServletRequest request = mock(HttpServletRequest.class);
        Notification notification = createNotification(22L, auctionId, NotificationType.BUY_NOW_WIN);
        Auction auction = createAuctionWithImage(auctionId, 25000L);

        // 토큰 파싱 -> 알림 조회 -> 경매 조회 흐름 mock 설정
        when(jwtTokenProvider.getTokenFromRequest(Objects.requireNonNull(request))).thenReturn(accessToken);
        when(jwtTokenProvider.getUserIdFromToken(accessToken)).thenReturn(userId);
        when(notificationRepository.findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(List.of(notification));
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        // when
        List<NotificationResponseDTO> result = notificationService.getNotifications(request);

        // then
        assertThat(result).hasSize(1);
        NotificationResponseDTO response = result.stream().findFirst().orElseThrow();
        assertThat(response).isNotNull();
        assertThat(response.getAuctionInfo()).isNotNull();
        assertThat(response.getType()).isEqualTo(NotificationType.BUY_NOW_WIN);
        assertThat(response.getAuctionInfo().getSuccessfulPrice()).isEqualTo(25000L);
        assertThat(response.getMyBidInfo()).isNull();
        verify(redisBidRepository, never()).findByAuctionIdOrderByBidAmountDesc(auctionId);
    }

    /**
     * JPA 저장 없이 Notification 엔티티 테스트 데이터를 만든다.
     * Reflection으로 id/감사 필드를 강제로 세팅한다.
     */
    private Notification createNotification(Long id, Long auctionId, NotificationType type) {
        Notification notification = Notification.builder()
                .auctionId(auctionId)
                .type(type)
                .build();

        ReflectionTestUtils.setField(Objects.requireNonNull(notification), "id", id);
        ReflectionTestUtils.setField(Objects.requireNonNull(notification), "createdAt", LocalDateTime.now().minusMinutes(1));
        ReflectionTestUtils.setField(Objects.requireNonNull(notification), "updatedAt", LocalDateTime.now());

        return notification;
    }

    /**
     * NotificationService 조립에 필요한 최소 경매/이미지 구조를 만든다.
     */
    private Auction createAuctionWithImage(Long auctionId, Long successfulPrice) {
        User seller = User.builder()
                .email("seller@test.com")
                .password("encoded-password")
                .name("seller")
                .phone("01012345678")
                .nickname("seller")
                .role(UserRole.USER)
                .build();

        Category category = Category.builder()
                .name("test-category")
                .build();

        Auction auction = Auction.builder()
                .user(seller)
                .category(category)
                .title("테스트 경매")
                .description("테스트 설명")
                .startPrice(1000L)
                .buyNowPrice(50000L)
                .currentPrice(successfulPrice)
                .successfulPrice(successfulPrice)
                .auctionStartTime(LocalDateTime.now().minusHours(1))
                .auctionEndTime(LocalDateTime.now().plusHours(1))
                .status(AuctionStatus.ACTIVE)
                .build();

        ReflectionTestUtils.setField(Objects.requireNonNull(auction), "id", auctionId);

        Image image = Image.builder()
                .auction(auction)
                .fileName("sample.jpg")
                .filePath("sample.jpg")
                .fileType("image/jpeg")
                .fileSize(100L)
                .build();

        auction.setImages(List.of(image));
        return auction;
    }
}
