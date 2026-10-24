package com.yantrakavach.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class DeviceCreateRequest {

    @NotBlank
    private String deviceId;

    @NotBlank
    private String machineId;

    private String label;

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }
}
