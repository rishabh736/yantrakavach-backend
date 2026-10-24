package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** JSON form matches the frontend's Severity union: info | warning | high | critical. */
public enum Severity {
    INFO, WARNING, HIGH, CRITICAL;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static Severity fromJson(String value) {
        return Severity.valueOf(value.trim().toUpperCase());
    }

    public int rank() {
        return ordinal();
    }
}
