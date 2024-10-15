package com.inhatc.auction.dto;

import com.inhatc.auction.common.Role;
import com.inhatc.auction.domain.User;

import lombok.Data;

@Data
public class UserRequestDTO {
    private String email;
    private String password;
    private String confirmPassword;
    private String name;
    private String phone;
    private Role role;
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
