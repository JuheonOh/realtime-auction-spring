package com.inhatc.auction.domain.auth.service;

import java.time.LocalDateTime;
import java.util.HashMap;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.inhatc.auction.domain.auth.dto.request.AuthRequestDTO;
import com.inhatc.auction.domain.auth.dto.response.AuthResponseDTO;
import com.inhatc.auction.domain.auth.entity.Auth;
import com.inhatc.auction.domain.auth.repository.AuthRepository;
import com.inhatc.auction.domain.user.dto.request.UserRequestDTO;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;
import com.inhatc.auction.global.constant.Role;
import com.inhatc.auction.global.constant.SecurityConstants;
import com.inhatc.auction.global.exception.CustomResponseStatusException;
import com.inhatc.auction.global.security.jwt.CustomUserDetails;
import com.inhatc.auction.global.security.jwt.JwtTokenProvider;

import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 로그인
     */
    @Transactional
    public AuthResponseDTO login(AuthRequestDTO requestDTO) {
        // 이메일과 비밀번호 확인
        User user = this.userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> {
                    HashMap<String, String> errors = new HashMap<>();
                    errors.put("email", "이메일과 비밀번호를 다시 확인해주세요.");
                    return new CustomResponseStatusException(HttpStatus.UNAUTHORIZED, errors);
                });

        if (!this.passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            HashMap<String, String> errors = new HashMap<>();
            errors.put("password", "이메일과 비밀번호를 다시 확인해주세요.");
            throw new CustomResponseStatusException(HttpStatus.UNAUTHORIZED, errors);
        }

        // 액세스 토큰 및 리프레시 토큰 생성
        String accessToken = this.jwtTokenProvider.generateAccessToken(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), user.getPassword()));
        String refreshToken = this.jwtTokenProvider.generateRefreshToken(
                new UsernamePasswordAuthenticationToken(new CustomUserDetails(user), user.getPassword()));

        // 이미 존재하는 Auth 엔티티가 있는 경우, 업데이트
        if (this.authRepository.existsByUser(user)) {
            user.getAuth().updateAccessToken(accessToken);
            user.getAuth().updateRefreshToken(refreshToken);
            return new AuthResponseDTO(user.getAuth());
        }

        // 존재하지 않는 Auth 엔티티인 경우, Auth 엔티티 및 토큰 저장
        Auth auth = Auth.builder()
                .user(user)
                .tokenType(SecurityConstants.TOKEN_TYPE.strip())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        this.authRepository.save(auth);

        return new AuthResponseDTO(auth);
    }

    /**
     * 회원가입
     */
    @Transactional
    public void signup(UserRequestDTO requestDTO) {
        log.info("회원가입 요청 : {}", requestDTO);

        // 휴대폰 번호 하이픈 제거
        requestDTO.setPhone(requestDTO.getPhone().replace("-", ""));

        // 비밀번호 암호화
        requestDTO.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        // 유저 엔티티 생성
        User user = User.builder()
                .email(requestDTO.getEmail())
                .password(requestDTO.getPassword())
                .name(requestDTO.getName())
                .phone(requestDTO.getPhone())
                .nickname(requestDTO.getNickname())
                .role(Role.ROLE_USER)
                .build();

        // 저장
        this.userRepository.save(user);
    }

    /**
     * 로그아웃
     */
    @Transactional
    public void logout(String refreshToken) {
        try {
            if (this.jwtTokenProvider.validateToken(refreshToken)) {
                Auth auth = this.authRepository.findByRefreshToken(refreshToken).orElse(null);

                if (auth != null) {
                    this.authRepository.delete(auth);
                }
            }
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    /**
     * Token 갱신
     */
    @Transactional
    public String refreshToken(String refreshToken) {
        // REFRESH_TOKEN 만료 확인 및 ACCESS_TOKEN 갱신
        try {
            if (this.jwtTokenProvider.validateToken(refreshToken)) {
                Auth auth = this.authRepository.findByRefreshToken(refreshToken).orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다. (REFRESH_TOKEN)"));

                String newAccessToken = this.jwtTokenProvider.generateAccessToken(
                        new UsernamePasswordAuthenticationToken(
                                new CustomUserDetails(auth.getUser()), auth.getUser().getPassword()));
                auth.updateAccessToken(newAccessToken);
                return newAccessToken;
            }
        } catch (ExpiredJwtException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return null;
    }
}