package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.dto.AnalyticsDto;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private static final Set<String> VALID_RANGES = Set.of("7D", "30D", "90D");

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public AnalyticsDto get(@RequestParam(name = "range", defaultValue = "30D") String range) {
        String normalized = range.trim().toUpperCase();
        if (!VALID_RANGES.contains(normalized)) {
            throw ApiException.badRequest("range must be one of 7D, 30D, 90D");
        }
        return analyticsService.build(normalized);
    }
}
