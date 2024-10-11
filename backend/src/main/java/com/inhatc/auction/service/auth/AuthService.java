package com.inhatc.auction.service.auth;

import com.inhatc.auction.common.Role;
import com.inhatc.auction.config.auth.CustomUserDetails;
import com.inhatc.auction.config.auth.JwtTokenProvider;
import com.inhatc.auction.domain.auth.Auth;
import com.inhatc.auction.domain.auth.User;
import com.inhatc.auction.dto.auth.AuthRequestDTO;
import com.inhatc.auction.dto.auth.AuthResponseDTO;
import com.inhatc.auction.dto.auth.UserRequestDTO;
import com.inhatc.auction.repository.auth.AuthRepository;
import com.inhatc.auction.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    @Transactional
    public void signup(UserRequestDTO requestDTO) {
        // 이메일 중복 확인
        if (this.userRepository.existsByEmail(requestDTO.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 존재하는 이메일입니다.");
        }

        // 비밀번호 확인
        if (!requestDTO.getPassword().equals(requestDTO.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.");
        }

        // 이용약관 및 개인정보 처리방침 동의 확인
        if (!requestDTO.isAgreeTerms()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이용약관과 개인정보 처리방침에 동의해주세요.");
        }

        // 권한 설정
        requestDTO.setRole(Role.ROLE_USER);

        // 비밀번호 암호화
        requestDTO.setPassword(passwordEncoder.encode(requestDTO.getPassword()));

        // 저장
        this.userRepository.save(requestDTO.toEntity());
    }

    /**
     * 로그인
     */
    @Transactional
    public AuthResponseDTO login(AuthRequestDTO requestDTO) {
        // 이메일과 비밀번호 확인
        User user = this.userRepository.findByEmail(requestDTO.getEmail())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디와 비밀번호를 다시 확인해주세요."));


        if (!this.passwordEncoder.matches(requestDTO.getPassword(), user.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디와 비밀번호를 다시 확인해주세요.");
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
        Auth auth = this.authRepository.save(Auth.builder()
                .user(user)
                .tokenType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build());
        return new AuthResponseDTO(auth);
    }


    /**
     * Token 갱신
     */
    @Transactional
    public String refreshToken(String refreshToken) {
        // REFRESH_TOKEN 만료 확인 및 ACCESS_TOKEN 갱신
        if (this.jwtTokenProvider.validateToken(refreshToken)) {
            Auth auth = this.authRepository.findByRefreshToken(refreshToken).orElseThrow(
                    () -> new IllegalArgumentException("해당 REFRESH_TOKEN 을 찾을 수 없습니다.\nREFRESH_TOKEN = " + refreshToken));

            String newAccessToken = this.jwtTokenProvider.generateAccessToken(
                    new UsernamePasswordAuthenticationToken(
                            new CustomUserDetails(auth.getUser()), auth.getUser().getPassword()));
            auth.updateAccessToken(newAccessToken);
            return newAccessToken;
        }

        // 사용 가능하지 않은 REFRESH_TOKEN 인 경우, ACCESS_TOKEN 및 REFRESH_TOKEN 재발급
        // 이 경우 사용자가 다시 로그인해야 하므로 재발급은 적절하지 않음
        return null;
    }
}
