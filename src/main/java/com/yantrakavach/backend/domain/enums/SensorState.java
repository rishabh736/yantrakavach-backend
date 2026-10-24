package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** JSON form matches the frontend's Sensor['state'] union: online | low-battery | offline. */
public enum SensorState {
    ONLINE("online"), LOW_BATTERY("low-battery"), OFFLINE("offline");

    private final String label;

    SensorState(String label) {
        this.label = label;
    }

    @JsonValue
    public String toJson() {
        return label;
    }

    @JsonCreator
    public static SensorState fromJson(String value) {
        for (SensorState s : values()) {
            if (s.label.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown sensor state: " + value);
    }
}
