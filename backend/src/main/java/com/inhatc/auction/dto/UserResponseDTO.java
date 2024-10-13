package com.inhatc.auction.dto;

import com.inhatc.auction.domain.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UserResponseDTO {
    private Long id;
    private String email;
    private String name;
    private String phone;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Builder
    public UserResponseDTO(User entity) {
        this.id = entity.getId();
        this.email = entity.getEmail();
        this.name = entity.getName();
        this.phone = entity.getPhone();
        // Enum -> String
        this.role = entity.getRole().name();
        this.createdAt = entity.getCreatedAt();
        this.updatedAt = entity.getUpdatedAt();
    }
}
