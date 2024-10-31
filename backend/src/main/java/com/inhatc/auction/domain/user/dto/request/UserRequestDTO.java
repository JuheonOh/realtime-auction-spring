package com.inhatc.auction.domain.user.dto.request;

import com.inhatc.auction.domain.user.validation.annotation.EmailUnique;
import com.inhatc.auction.domain.user.validation.annotation.NicknameUnique;
import com.inhatc.auction.domain.user.validation.annotation.PasswordMatches;
import com.inhatc.auction.global.validation.annotation.ValidPhoneNumber;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@PasswordMatches
public class UserRequestDTO {
    @NotBlank(message = "이메일을 입력해 주세요.")
    @Email(message = "이메일 형식이 올바르지 않습니다.")
    @EmailUnique
    private String email;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String password;

    @NotBlank(message = "비밀번호를 입력해 주세요.")
    private String confirmPassword;

    @NotBlank(message = "이름을 입력해 주세요.")
    private String name;

    @NotBlank(message = "전화번호를 입력해 주세요.")
    @ValidPhoneNumber
    private String phone;

    @NotBlank(message = "닉네임을 입력해 주세요.")
    @NicknameUnique
    private String nickname;

    @AssertTrue(message = "이용약관과 개인정보처리방침에 동의해야 합니다.")
    private boolean agreeTerms;
}
