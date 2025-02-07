package com.inhatc.auction.domain.user.dto.response;

import java.time.LocalDateTime;

import com.inhatc.auction.domain.user.entity.User;

import lombok.Builder;
import lombok.Data;

@Data
public class UserResponseDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String nickname;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserResponseDTO(User entity) {
        this.id = entity.getId();
        this.email = entity.getEmail();
        this.name = entity.getName();
        this.phone = entity.getPhone();
        this.nickname = entity.getNickname();
        this.role = entity.getRole().name();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
