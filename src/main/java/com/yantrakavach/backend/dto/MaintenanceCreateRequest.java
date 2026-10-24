package com.yantrakavach.backend.dto;

import com.yantrakavach.backend.domain.enums.MaintenanceStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Matches the body the frontend's addMaintenance()/LogMaintenanceModal sends. */
public class MaintenanceCreateRequest {

    @NotBlank
    private String machineId;
    @NotBlank
    private String issue;
    @NotBlank
    private String action;
    @NotBlank
    private String technician;
    @NotNull
    private MaintenanceStatus status;
    private int downtimeMin;
    private int cost;
    private String parts;

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

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
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
}
