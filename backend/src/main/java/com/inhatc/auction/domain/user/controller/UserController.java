package com.inhatc.auction.domain.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.user.dto.response.UserResponseDTO;
import com.inhatc.auction.domain.user.service.UserService;
import com.inhatc.auction.global.jwt.JwtTokenProvider;

import jakarta.servlet.http.HttpServletRequest;
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
    public ResponseEntity<?> findUser(@NonNull HttpServletRequest request) {
        String accessToken = this.jwtTokenProvider.getTokenFromRequest(request);
        if (accessToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }

        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken);
        UserResponseDTO userResponseDTO = this.userService.findById(id);

        return ResponseEntity.status(HttpStatus.OK).body(userResponseDTO);
    }
}
