package com.inhatc.auction.common.validation;

import com.inhatc.auction.dto.UserRequestDTO;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordMatchesValidator implements ConstraintValidator<PasswordMatches, UserRequestDTO> {

    @Override
    public boolean isValid(UserRequestDTO dto, ConstraintValidatorContext context) {
        return dto.getPassword() != null && dto.getPassword().equals(dto.getConfirmPassword());
    }
}
