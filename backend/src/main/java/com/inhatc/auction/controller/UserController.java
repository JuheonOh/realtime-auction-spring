package com.inhatc.auction.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.inhatc.auction.config.SecurityConstants;
import com.inhatc.auction.config.jwt.JwtTokenProvider;
import com.inhatc.auction.dto.UserRequestDTO;
import com.inhatc.auction.dto.UserResponseDTO;
import com.inhatc.auction.service.UserService;

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
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        UserResponseDTO UserResponseDTO = this.userService.findById(id);

        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO);
    }

    /**
     * 회원정보 수정 API
     */
    @PutMapping
    public ResponseEntity<?> updateUser(@RequestHeader("Authorization") String header,
            @RequestBody UserRequestDTO requestDTO) {
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        this.userService.update(id, requestDTO);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    /**
     * 회원정보 삭제 API
     */
    @DeleteMapping
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String header) {
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        this.userService.delete(id);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}