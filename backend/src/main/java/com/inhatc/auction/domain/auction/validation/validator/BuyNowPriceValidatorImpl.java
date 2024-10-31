package com.inhatc.auction.domain.auction.validation.validator;

import com.inhatc.auction.domain.auction.dto.request.AuctionRequestDTO;
import com.inhatc.auction.domain.auction.validation.annotation.BuyNowPriceValidator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BuyNowPriceValidatorImpl implements ConstraintValidator<BuyNowPriceValidator, AuctionRequestDTO> {

    @Override
    public boolean isValid(AuctionRequestDTO requestDTO, ConstraintValidatorContext context) {
        // buyNowPrice가 null이거나 0이면 검증하지 않음
        if (requestDTO.getBuyNowPrice() == null || requestDTO.getBuyNowPrice() == 0) {
            return true;
        }

        // startPrice가 null이면 다른 검증 어노테이션에서 처리
        if (requestDTO.getStartPrice() == null) {
            return true;
        }

        // buyNowPrice가 startPrice보다 작으면 검증 실패
        boolean isValid = requestDTO.getBuyNowPrice() >= requestDTO.getStartPrice();
        if (!isValid) {
            context.disableDefaultConstraintViolation(); // 기본 메시지 비활성화
            context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate()) // 새로운 메시지 생성
                    .addPropertyNode("buyNowPrice").addConstraintViolation(); // buyNowPrice 속성에 대한 메시지 추가
        }

        return isValid;
    }
}