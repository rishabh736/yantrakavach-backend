package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Status {
    HEALTHY, WARNING, CRITICAL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Status fromJson(String value) {
        return Status.valueOf(value.trim().toUpperCase());
    }
}
