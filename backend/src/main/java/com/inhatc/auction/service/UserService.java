package com.inhatc.auction.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.inhatc.auction.domain.User;
import com.inhatc.auction.dto.UserRequestDTO;
import com.inhatc.auction.dto.UserResponseDTO;
import com.inhatc.auction.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

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

        this.userRepository.save(user);
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
