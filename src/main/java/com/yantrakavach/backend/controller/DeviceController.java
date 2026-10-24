package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.dto.DeviceCreateRequest;
import com.yantrakavach.backend.dto.DeviceCreatedDto;
import com.yantrakavach.backend.dto.DeviceDto;
import com.yantrakavach.backend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Provisioning and management for ESP32 / sensor-gateway devices. Not called by browsers directly. */
@RestController
@RequestMapping("/api/devices")
@PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceDto> list() {
        return deviceService.list();
    }

    /** The response's apiKey is shown once. Copy it into the device's firmware immediately. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DeviceCreatedDto create(@Valid @RequestBody DeviceCreateRequest body) {
        return deviceService.provision(body);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@PathVariable("id") String id) {
        deviceService.revoke(id);
    }
}
