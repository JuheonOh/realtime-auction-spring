package com.inhatc.auction.controller.auth;

import com.inhatc.auction.config.auth.JwtTokenProvider;
import com.inhatc.auction.dto.auth.UserRequestDTO;
import com.inhatc.auction.dto.auth.UserResponseDTO;
import com.inhatc.auction.service.auth.UserService;
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
    public ResponseEntity<?> findUser(@RequestHeader("Authorization") String accessToken) {
        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken.substring(7));
        UserResponseDTO UserResponseDTO = this.userService.findById(id);
        return ResponseEntity.status(HttpStatus.OK).body(UserResponseDTO);
    }

    /**
     * 회원정보 수정 API
     */
    @PutMapping("/user")
    public ResponseEntity<?> updateUser(@RequestHeader("Authorization") String accessToken,
                                        @RequestBody UserRequestDTO requestDTO) {
        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken.substring(7));
        this.userService.update(id, requestDTO);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }

    /**
     * 회원정보 삭제 API
     */
    @DeleteMapping("/user")
    public ResponseEntity<?> deleteUser(@RequestHeader("Authorization") String accessToken) {
        Long id = this.jwtTokenProvider.getUserIdFromToken(accessToken.substring(7));
        this.userService.delete(id);
        return ResponseEntity.status(HttpStatus.OK).body(null);
    }
}