package com.yantrakavach.backend.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A physical ESP32 (or any other HTTP-capable gateway) registered to report readings for one machine.
 * One device is expected to cover all the sensors wired to a single machine; it authenticates with the
 * API key issued when it was provisioned, not with a user login.
 */
@Entity
@Table(name = "devices")
public class Device {

    @Id
    private String id; // e.g. "ESP32-01" — chosen when provisioned, not auto-generated

    private String machineId;
    private String label;

    @Column(unique = true, nullable = false)
    private String apiKeyHash; // SHA-256 hex of the device's raw API key — the raw key itself is never stored

    private Instant lastSeenAt;

    public Device() {
    }

    public Device(String id, String machineId, String label, String apiKeyHash) {
        this.id = id;
        this.machineId = machineId;
        this.label = label;
        this.apiKeyHash = apiKeyHash;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    public String getApiKeyHash() {
        return apiKeyHash;
    }

    public void setApiKeyHash(String apiKeyHash) {
        this.apiKeyHash = apiKeyHash;
    }

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
