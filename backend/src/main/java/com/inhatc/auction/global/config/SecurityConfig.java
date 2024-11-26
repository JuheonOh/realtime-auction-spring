package com.inhatc.auction.global.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.inhatc.auction.global.exception.CustomAccessDeniedHandler;
import com.inhatc.auction.global.exception.CustomAuthenticationEntryPoint;
import com.inhatc.auction.global.jwt.JwtTokenFilter;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtTokenFilter jwtTokenFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // csrf disable
                .formLogin(AbstractHttpConfigurer::disable) // Form 로그인 방식 disable
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 방식 disable

                // 세션 관리 정책 설정
                // 세션 인증을 사용하지 않고, JWT 토큰을 사용하기 때문에 세션을 생성하지 않음
                .sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // url 권한 설정
                // 인증이 필요 없는 url 설정
                // 그 외 모든 요청은 인증이 필요함
                .authorizeHttpRequests(
                        requests -> requests.requestMatchers("/**").permitAll().anyRequest().authenticated())

                // 예외 처리 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint()) // 인증이 필요한 경우
                        .accessDeniedHandler(new CustomAccessDeniedHandler())) // 인가가 필요한 경우

                // 토큰 인증 필터 추가
                .addFilterBefore(this.jwtTokenFilter, UsernamePasswordAuthenticationFilter.class)

                // CORS 설정
                .cors(corsCustomizer -> corsCustomizer.configurationSource(corsConfigurationSource()));
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*")); // 모든 출처 패턴 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")); // 모든 메소드 허용
        configuration
                .setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "REFRESH_TOKEN"));
        configuration.setExposedHeaders(Arrays.asList("Authorization", "REFRESH_TOKEN", "Set-Cookie")); // 노출 헤더
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}