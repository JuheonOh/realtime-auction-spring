package com.inhatc.auction.domain.auction.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.inhatc.auction.domain.auction.dto.request.AuctionRequestDTO;
import com.inhatc.auction.domain.auction.service.AuctionService;
import com.inhatc.auction.domain.user.entity.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class AuctionControllerTests {

    @Mock
    private AuctionService auctionService;

    @InjectMocks
    private AuctionController auctionController;

    @Test
    void createAuction_usesOnlyAuthenticatedCustomUserDetailsIdForSellerIdentity() {
        Long authenticatedUserId = 42L;
        AuctionRequestDTO requestDTO = new AuctionRequestDTO();
        CustomUserDetails userDetails = mock(CustomUserDetails.class);
        when(userDetails.getId()).thenReturn(authenticatedUserId);
        when(auctionService.createAuction(authenticatedUserId, requestDTO)).thenReturn(100L);

        auctionController.createAuction(userDetails, requestDTO);

        verify(auctionService).createAuction(authenticatedUserId, requestDTO);
    }

    @Test
    void auctionRequestDto_hasNoUserIdFieldThatCouldOverrideAuthenticatedSellerIdentity() {
        assertThat(Arrays.stream(AuctionRequestDTO.class.getDeclaredFields())
                .map(Field::getName))
                .doesNotContain("userId");
    }
}
