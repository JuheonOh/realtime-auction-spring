package com.inhatc.auction.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = NicknameUniqueValidator.class)
@Target({ ElementType.FIELD })
@Retention(RetentionPolicy.RUNTIME)
public @interface NicknameUnique {
    String message() default "이미 존재하는 닉네임입니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
