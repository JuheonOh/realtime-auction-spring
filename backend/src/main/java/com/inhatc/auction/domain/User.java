package com.inhatc.auction.domain;

import com.inhatc.auction.common.constant.Role;
import com.inhatc.auction.dto.UserRequestDTO;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String email;
    private String password;
    private String name;
    private String phone;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Auth auth;

    @OneToMany(mappedBy = "user")
    private List<Auction> auctions;

    @OneToMany(mappedBy = "user")
    private List<Bid> bids;

    @Builder
    public User(String email, String password, String name, String phone, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // public void update(UserRequestDTO requestDTO) {
    // this.email = requestDTO.getEmail();
    // this.password = requestDTO.getPassword();
    // this.name = requestDTO.getName();
    // this.phone = requestDTO.getPhone();
    // this.updatedAt = LocalDateTime.now();
    // }
}
