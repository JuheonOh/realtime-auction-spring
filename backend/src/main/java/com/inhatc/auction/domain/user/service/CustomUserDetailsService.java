package com.inhatc.auction.domain.user.service;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.inhatc.auction.domain.user.entity.CustomUserDetails;
import com.inhatc.auction.domain.user.entity.User;
import com.inhatc.auction.domain.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String name) throws UsernameNotFoundException {
        String userName = Objects.requireNonNull(name, "name must not be null");
        User user = userRepository.findByName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저가 존재하지 않습니다. name = " + userName));
        return new CustomUserDetails(user);
    }

    public UserDetails loadUserById(@NonNull Long id) throws IllegalArgumentException {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. id = " + id));
        return new CustomUserDetails(user);
    }

    public UserDetails loadUserByEmail(@NonNull String email) throws IllegalArgumentException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("해당 유저가 존재하지 않습니다. email = " + email));
        return new CustomUserDetails(user);
    }
}
