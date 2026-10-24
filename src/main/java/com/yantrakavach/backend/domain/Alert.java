package com.yantrakavach.backend.domain;

import com.yantrakavach.backend.domain.enums.AlertStatus;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.Severity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Field names match the frontend's Alert interface: id, machineId, severity, param, message, createdAt, status, technician. */
@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    private String id; // "A-1042"

    private String machineId;

    @Enumerated(EnumType.STRING)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    private ParamKey param;

    private String message;
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    private AlertStatus status;

    private String technician;

    public Alert() {
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

    public Severity getSeverity() {
        return severity;
    }

    public void setSeverity(Severity severity) {
        this.severity = severity;
    }

    public ParamKey getParam() {
        return param;
    }

    public void setParam(ParamKey param) {
        this.param = param;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public AlertStatus getStatus() {
        return status;
    }

    public void setStatus(AlertStatus status) {
        this.status = status;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }
}
