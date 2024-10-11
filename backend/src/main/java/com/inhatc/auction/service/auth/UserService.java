package com.inhatc.auction.service.auth;

import com.inhatc.auction.domain.auth.User;
import com.inhatc.auction.dto.auth.UserRequestDTO;
import com.inhatc.auction.dto.auth.UserResponseDTO;
import com.inhatc.auction.repository.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;



    /**
     * User 조회
     */
    @Transactional
    public UserResponseDTO findById(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. user_id = " + id));

        return new UserResponseDTO(user);
    }

    /**
     * User 수정
     */
    @Transactional
    public void update(Long id, UserRequestDTO requestDTO) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. user_id = " + id));
        user.update(requestDTO);
    }

    /**
     * User 삭제
     */
    @Transactional
    public void delete(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다. user_id = " + id));
        this.userRepository.delete(user);
    }

}
