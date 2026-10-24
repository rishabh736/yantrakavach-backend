package com.yantrakavach.backend.dto;

import java.util.List;

/** Matches the frontend's AnalyticsData interface in src/lib/mock.ts. */
public record AnalyticsDto(
        List<String> labels,
        List<Double> uptime,
        List<Double> downtime,
        List<Integer> failures,
        List<Double> energy,
        List<Integer> maintenance,
        TempTrend temp,
        VibTrend vib,
        Kpi kpi
) {
    public record TempTrend(List<Double> cnc, List<Double> press, List<Double> motor) {
    }

    public record VibTrend(List<Double> cnc, List<Double> press, List<Double> compressor) {
    }

    public record Kpi(double oee, double mtbfHours, double mttrHours, long energyCost, long costAvoided, int planned) {
    }
}
