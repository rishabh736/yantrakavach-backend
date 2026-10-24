package com.yantrakavach.backend.dto;

import jakarta.validation.constraints.NotBlank;

public class AssignRequest {

    @NotBlank
    private String technician;

    public String getTechnician() {
        return technician;
    }

    public void setTechnician(String technician) {
        this.technician = technician;
    }
}
