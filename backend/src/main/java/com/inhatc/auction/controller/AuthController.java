package com.inhatc.auction.controller;

import com.inhatc.auction.dto.AuthRequestDTO;
import com.inhatc.auction.dto.AuthResponseDTO;
import com.inhatc.auction.dto.UserRequestDTO;
import com.inhatc.auction.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Log4j2
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequestDTO requestDTO) {
        AuthResponseDTO responseDTO = this.authService.login(requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(responseDTO);
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody UserRequestDTO requestDTO) {
        this.authService.signup(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(null);
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestHeader("REFRESH_TOKEN") String refreshToken) {
        String newAccessToken = this.authService.refreshToken(refreshToken);

        Map<String, String> response = new HashMap<>();
        response.put("accessToken", newAccessToken);

        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
