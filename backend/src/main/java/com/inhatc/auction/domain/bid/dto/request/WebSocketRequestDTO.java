package com.inhatc.auction.domain.bid.dto.request;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WebSocketRequestDTO {
    private String type;
    private String accessToken;
    private Map<String, String> data;
}
