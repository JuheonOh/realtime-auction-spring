package com.inhatc.auction.domain;

import java.time.LocalDateTime;
import java.util.List;

import com.inhatc.auction.common.constant.Role;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Auth auth;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Auction> auctions;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Bid> bids;

    @Builder
    public User(String email, String password, String name, String phone, String nickname, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.phone = phone;
        this.nickname = nickname;
        this.role = role;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
