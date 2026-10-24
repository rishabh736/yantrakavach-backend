package com.yantrakavach.backend.dto;

import java.time.Instant;

/** Device listing — never includes the API key. */
public record DeviceDto(String deviceId, String machineId, String label, boolean online, Instant lastSeenAt) {
}
