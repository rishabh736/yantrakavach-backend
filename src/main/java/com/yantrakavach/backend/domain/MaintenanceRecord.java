package com.yantrakavach.backend.domain;

import com.yantrakavach.backend.domain.enums.MaintenanceStatus;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Field names match the frontend's MaintenanceRecord interface. */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    @Id
    private String id; // "WO-2601"

    private String machineId;
    private String issue;
    private String action;
    private String technician;
    private Instant date;
    private int downtimeMin;
    private int cost;
    private String parts;

    @Enumerated(EnumType.STRING)
    private MaintenanceStatus status;

    public MaintenanceRecord() {
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

    public String getIssue() {
        return issue;
    }

    public void setIssue(String issue) {
        this.issue = issue;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }

    public Instant getDate() {
        return date;
    }

    public void setDate(Instant date) {
        this.date = date;
    }

    public int getDowntimeMin() {
        return downtimeMin;
    }

    public void setDowntimeMin(int downtimeMin) {
        this.downtimeMin = downtimeMin;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }

    public String getParts() {
        return parts;
    }

    public void setParts(String parts) {
        this.parts = parts;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }
}
