package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.AppUser;
import com.yantrakavach.backend.dto.LoginResponse;
import com.yantrakavach.backend.dto.UserDto;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.repo.UserRepository;
import com.yantrakavach.backend.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public LoginResponse login(String email, String password) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("Email or password does not match."));
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw ApiException.unauthorized("Email or password does not match.");
        }
        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().name(), user.getName());
        return new LoginResponse(token, new UserDto(user.getName(), user.getEmail(), user.getRole()));
    }

    public UserDto me(String email) {
        AppUser user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("User not found."));
        return new UserDto(user.getName(), user.getEmail(), user.getRole());
    }
}
