package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** The five monitored machine parameters. JSON form matches the frontend's ParamKey union exactly. */
public enum ParamKey {
    VIBRATION, TEMPERATURE, SOUND, CURRENT, VOLTAGE;

    @JsonValue
    public String toJson() {
        return name().toLowerCase();
    }

    @JsonCreator
    public static ParamKey fromJson(String value) {
        return ParamKey.valueOf(value.trim().toUpperCase());
    }
}
