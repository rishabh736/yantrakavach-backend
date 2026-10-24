package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.dto.LoginRequest;
import com.yantrakavach.backend.dto.LoginResponse;
import com.yantrakavach.backend.dto.UserDto;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest body) {
        return authService.login(body.getEmail(), body.getPassword());
    }

    /** Returns the user for the Bearer token on the request, or 401 if there isn't a valid one. */
    @GetMapping("/me")
    public UserDto me(Authentication authentication) {
        if (authentication == null) {
            throw ApiException.unauthorized("No valid session.");
        }
        return authService.me(authentication.getName());
    }
}
