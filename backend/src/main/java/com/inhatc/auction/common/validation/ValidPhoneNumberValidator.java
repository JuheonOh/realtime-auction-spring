package com.inhatc.auction.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    private static final String PHONE_PATTERN = "^01(?:0|1|[6-9])-(\\d{3}|\\d{4})-\\d{4}$";

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.trim().isEmpty()) {
            return false; // 빈 문자열이면 유효하지 않음
        }

        return phone.matches(PHONE_PATTERN);
    }
}