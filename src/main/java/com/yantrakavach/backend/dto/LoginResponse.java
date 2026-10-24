package com.yantrakavach.backend.dto;

public record LoginResponse(String token, UserDto user) {
}
