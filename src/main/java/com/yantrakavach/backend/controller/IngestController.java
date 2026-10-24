package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.dto.IngestRequest;
import com.yantrakavach.backend.dto.IngestResponse;
import com.yantrakavach.backend.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Where an ESP32 (or any other HTTP-capable sensor gateway) posts its readings. Authenticated with a
 * per-device API key in the X-Device-Key header — not a user login — because a microcontroller can't
 * sensibly hold a JWT refresh flow. See DeviceController for provisioning a key, and
 * /firmware/esp32-yantra-kavach.ino for a working client.
 */
@RestController
@RequestMapping("/api/ingest")
public class IngestController {

    private final DeviceService deviceService;

    public IngestController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping("/readings")
    public IngestResponse readings(@RequestHeader("X-Device-Key") String deviceKey, @Valid @RequestBody IngestRequest body) {
        return deviceService.ingest(deviceKey, body);
    }
}
