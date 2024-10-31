package com.inhatc.auction.domain.image.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ImageResponseDTO {
    private String filePath;
    private String fileName;
}
