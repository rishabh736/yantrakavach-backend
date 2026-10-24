package com.yantrakavach.backend.dto;

/**
 * Returned exactly once, at provisioning time. Copy apiKey into the device's firmware now —
 * it cannot be retrieved again afterwards (only reset, via re-provisioning).
 */
public record DeviceCreatedDto(String deviceId, String machineId, String label, String apiKey) {
}
