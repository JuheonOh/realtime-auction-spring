package com.inhatc.auction.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageResponseDTO {
    private String filePath;
    private String fileName;
}
