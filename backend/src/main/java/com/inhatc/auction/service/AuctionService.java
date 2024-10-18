package com.inhatc.auction.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.inhatc.auction.common.constant.AuctionStatus;
import com.inhatc.auction.domain.Auction;
import com.inhatc.auction.domain.Category;
import com.inhatc.auction.domain.Image;
import com.inhatc.auction.domain.User;
import com.inhatc.auction.dto.AuctionRequestDTO;
import com.inhatc.auction.repository.AuctionRepository;
import com.inhatc.auction.repository.CategoryRepository;
import com.inhatc.auction.repository.UserRepository;

import io.jsonwebtoken.io.IOException;
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

    public List<Auction> getAuctionList() {
        return auctionRepository.findAll();
    }

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

                            String fileSaveName = UUID.randomUUID().toString() + "_" + image.getOriginalFilename();
                            String fileRealName = image.getOriginalFilename();
                            String fileType = image.getContentType();
                            long fileSize = image.getSize();
                            Path uploadDir = Paths.get(uploadPath);

                            if (!Files.exists(uploadDir)) {
                                Files.createDirectories(uploadDir);
                            }

                            if (fileType != null && !fileType.equals("image/jpeg") && !fileType.equals("image/png")
                                    && !fileType.equals("image/jpg") && !fileType.equals("image/gif")
                                    && !fileType.equals("image/bmp") && !fileType.equals("image/webp")) {
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
                        } catch (IOException | IllegalStateException | java.io.IOException e) {
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

    public Auction getAuctionDetail(Long auctionId) {
        return auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("경매를 찾을 수 없습니다"));
    }
}
