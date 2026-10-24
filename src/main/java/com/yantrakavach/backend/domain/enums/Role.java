package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** JSON form matches the frontend's Role union: Admin | Maintenance Manager | Operator. */
public enum Role {
    ADMIN("Admin"), MAINTENANCE_MANAGER("Maintenance Manager"), OPERATOR("Operator");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    @JsonValue
    public String toJson() {
        return label;
    }

    @JsonCreator
    public static Role fromJson(String value) {
        for (Role r : values()) {
            if (r.label.equalsIgnoreCase(value) || r.name().equalsIgnoreCase(value)) return r;
        }
        throw new IllegalArgumentException("Unknown role: " + value);
    }

    public String authority() {
        return "ROLE_" + name();
    }
}
