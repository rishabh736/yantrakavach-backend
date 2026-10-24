package com.yantrakavach.backend.dto;

import com.yantrakavach.backend.domain.enums.Role;

/** Matches the frontend's User interface: name, email, role. */
public record UserDto(String name, String email, Role role) {
}
