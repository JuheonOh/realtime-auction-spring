package com.inhatc.auction.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.inhatc.auction.domain.user.dto.request.UserRequestDTO;
import com.inhatc.auction.domain.user.dto.response.UserResponseDTO;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public UserResponseDTO findById(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. user_id = " + id));

        return new UserResponseDTO(user);
    }

    @Transactional
    public void update(Long id, UserRequestDTO requestDTO) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. user_id = " + id));

        this.userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        User user = this.userRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("해당 사용자를 찾을 수 없습니다. user_id = " + id));
        this.userRepository.delete(user);
    }

}