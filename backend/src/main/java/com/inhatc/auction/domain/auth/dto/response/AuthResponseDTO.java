package com.inhatc.auction.domain.auth.dto.response;

import com.inhatc.auction.domain.auth.entity.Auth;

import lombok.Builder;
import lombok.Data;

@Data
public class AuthResponseDTO {
    private String tokenType;
    private String accessToken;
    private String refreshToken;

    @Builder
    public AuthResponseDTO(Auth entity) {
        this.tokenType = entity.getTokenType();
        this.accessToken = entity.getAccessToken();
        this.refreshToken = entity.getRefreshToken();
    }
}
