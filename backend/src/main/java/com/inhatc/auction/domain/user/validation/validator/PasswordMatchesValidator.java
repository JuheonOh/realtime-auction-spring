package com.inhatc.auction.domain.user.validation.validator;

import com.inhatc.auction.domain.user.dto.request.UserRequestDTO;
import com.inhatc.auction.domain.user.validation.annotation.PasswordMatches;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserRequestDTO> {

    @Override
    public boolean isValid(UserRequestDTO dto, ConstraintValidatorContext context) {
        return dto.getPassword() != null && dto.getPassword().equals(dto.getConfirmPassword());
    }
}
