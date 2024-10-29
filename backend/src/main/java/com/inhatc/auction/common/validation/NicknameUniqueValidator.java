package com.inhatc.auction.common.validation;

import org.springframework.beans.factory.annotation.Autowired;

import com.inhatc.auction.repository.UserRepository;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NicknameUniqueValidator implements ConstraintValidator<NicknameUnique, String> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(String nickname, ConstraintValidatorContext context) {
        return nickname != null && !userRepository.existsByNickname(nickname);
    }
}