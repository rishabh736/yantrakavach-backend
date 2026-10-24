package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AlertStatus {
    OPEN, ACKNOWLEDGED, RESOLVED;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static AlertStatus fromJson(String value) {
        return AlertStatus.valueOf(value.trim().toUpperCase());
    }
}
