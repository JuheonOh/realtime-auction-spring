package com.inhatc.auction.domain.user.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.domain.user.dto.response.UserResponseDTO;
import com.inhatc.auction.domain.user.service.UserService;
import com.inhatc.auction.global.constant.JwtHeader;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원정보 조회 API
     */
    @GetMapping
    public ResponseEntity<?> findUser(@RequestHeader("Authorization") String header) {
        String accessToken = header.substring(JwtHeader.TOKEN_PREFIX.getValue().length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        UserResponseDTO userResponseDTO = this.userService.findById(id);

        return ResponseEntity.status(HttpStatus.OK).body(userResponseDTO);
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAllUser() {
        List<UserResponseDTO> userResponseDTOs = this.userService.findAll();

        return ResponseEntity.status(HttpStatus.OK).body(userResponseDTOs);
    }
}