package com.inhatc.auction.global.constant;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum JwtPayload {
    USER_ID("user-id"),
    USER_NAME("user-name"),
    USER_EMAIL("user-email"),
    USER_ROLE("user-role");

    private final String claims;
}
