package com.inhatc.auction.domain.auction.validation.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.inhatc.auction.domain.auction.validation.validator.BuyNowPriceValidatorImpl;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Constraint(validatedBy = BuyNowPriceValidatorImpl.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface BuyNowPriceValidator {
    String message() default "즉시 구매 가격은 경매 시작 가격보다 작을 수 없습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
