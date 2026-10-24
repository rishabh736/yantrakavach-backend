package com.yantrakavach.backend.domain.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/** JSON form matches the frontend's MaintenanceRecord['status'] union exactly. */
public enum MaintenanceStatus {
    SCHEDULED("Scheduled"), IN_PROGRESS("In Progress"), COMPLETED("Completed");

    private final String label;

    MaintenanceStatus(String label) {
        this.label = label;
    }

    @JsonValue
    public String toJson() {
        return label;
    }

    @JsonCreator
    public static MaintenanceStatus fromJson(String value) {
        for (MaintenanceStatus s : values()) {
            if (s.label.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value)) return s;
        }
        throw new IllegalArgumentException("Unknown maintenance status: " + value);
    }
}
