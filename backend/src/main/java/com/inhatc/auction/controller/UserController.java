package com.inhatc.auction.controller;

import com.inhatc.auction.config.SecurityConstants;
import com.inhatc.auction.config.jwt.JwtTokenProvider;
import com.inhatc.auction.dto.UserRequestDTO;
import com.inhatc.auction.dto.UserResponseDTO;
import com.inhatc.auction.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class UserController {
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원정보 조회 API
     */
    @GetMapping("/user")
    public ResponseEntity<?> findUser(@RequestHeader("Authorization") String header) {
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        UserResponseDTO UserResponseDTO = this.userService.findById(id);

        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO);
    }

    /**
     * 회원정보 수정 API
     */
    @PutMapping("/user")
    public ResponseEntity<?> updateUser(@RequestHeader("Authorization") String header, @RequestBody UserRequestDTO requestDTO) {
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        this.userService.update(id, requestDTO);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    /**
     * 회원정보 삭제 API
     */
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String header) {
        String accessToken = header.substring(SecurityConstants.TOKEN_PREFIX.length());

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        this.userService.delete(id);

        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}