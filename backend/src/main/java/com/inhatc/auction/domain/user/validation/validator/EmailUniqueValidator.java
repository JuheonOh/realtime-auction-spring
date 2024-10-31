package com.inhatc.auction.domain.user.validation.validator;

import org.springframework.beans.factory.annotation.Autowired;

import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.domain.user.validation.annotation.EmailUnique;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailUniqueValidator implements ConstraintValidator<EmailUnique, String> {

    @Autowired
    private UserRepository userRepository;

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        return email != null && !userRepository.existsByEmail(email);
    }
}
