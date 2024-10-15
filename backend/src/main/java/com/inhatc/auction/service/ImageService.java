package com.inhatc.auction.service;

import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.Image;
import com.inhatc.auction.repository.ImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class ImageService {
    private final ImageRepository imageRepository;

    public Image saveImage(Image image) {
        return imageRepository.save(image);
    }

    public Image getImage(Long id) {
        return imageRepository.findById(id).orElseThrow(() -> new RuntimeException("이미지를 찾을 수 없습니다"));
    }
}
