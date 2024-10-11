package com.inhatc.auction.dto.auth;

import com.inhatc.auction.common.Role;
import com.inhatc.auction.domain.auth.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {
    @NotNull(message = "이메일 입력은 필수입니다.")
    @Email
    private String email;

    @NotNull(message = "비밀번호 입력은 필수입니다.")
    private String password;

    @NotNull(message = "비밀번호 확인 입력은 필수입니다.")
    private String confirmPassword;

    @NotNull(message = "이름 입력은 필수입니다.")
    private String name;

    @NotNull(message = "전화번호 입력은 필수입니다.")
    private String phone;

    private Role role;

    @NotNull(message = "이용약관과 개인정보 처리방침에 동의해주세요.")
    private boolean agreeTerms;

    public User toEntity() {
        return User.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .phone(this.phone)
                .role(this.role)
                .build();
    }
}
