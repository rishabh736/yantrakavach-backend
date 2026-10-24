package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.dto.ThresholdPairDto;
import com.yantrakavach.backend.service.ThresholdService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Serves the live warning/critical thresholds as a plain JSON object keyed by lowercase parameter name
 * (vibration, temperature, sound, current, voltage), matching the frontend's Thresholds type exactly.
 */
@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final ThresholdService thresholdService;

    public SettingsController(ThresholdService thresholdService) {
        this.thresholdService = thresholdService;
    }

    @GetMapping("/thresholds")
    public Map<String, ThresholdPairDto> getThresholds() {
        return thresholdService.current();
    }

    @PutMapping("/thresholds")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    public Map<String, ThresholdPairDto> updateThresholds(@RequestBody Map<String, ThresholdPairDto> body) {
        thresholdService.update(body);
        return thresholdService.current();
    }
}
