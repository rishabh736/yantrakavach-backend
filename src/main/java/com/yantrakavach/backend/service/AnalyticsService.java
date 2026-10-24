package com.yantrakavach.backend.service;

import com.yantrakavach.backend.dto.AnalyticsDto;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleSupplier;

/**
 * Builds plant-wide analytics series. This mirrors the shape (not necessarily the exact numbers) of the
 * frontend's buildAnalytics() in src/lib/mock.ts, using the same seeded-random approach, so the backend
 * produces equally plausible, equally reproducible-per-range sample series until real historian data is
 * wired in (see AnalyticsService#fromHistorian for where that would plug in).
 */
@Service
public class AnalyticsService {

    private static final double ENERGY_RATE_PER_KWH = 7.6;

    public AnalyticsDto build(String range) {
        int n = switch (range) {
            case "7D" -> 7;
            case "30D" -> 10;
            default -> 12; // 90D
        };
        int stepDays = switch (range) {
            case "7D" -> 1;
            case "30D" -> 3;
            default -> 7;
        };
        double mul = switch (range) {
            case "7D" -> 1;
            case "30D" -> 3;
            default -> 7;
        };
        int planningDays = switch (range) {
            case "7D" -> 7;
            case "30D" -> 30;
            default -> 90;
        };

        MulberryRng r = new MulberryRng(MulberryRng.hashStr("an" + range));
        List<String> labels = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < n; i++) {
            ZonedDateTime d = now.minusSeconds((long) (n - 1 - i) * stepDays * 86400L).atZone(ZoneOffset.UTC);
            labels.add("7D".equals(range)
                    ? d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    : d.getDayOfMonth() + " " + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.ENGLISH));
        }

        DoubleSupplier u = r::next;
        List<Double> uptime = series(n, () -> 94 + u.getAsDouble() * 4.5);
        List<Double> downtime = scale(series(n, () -> 1 + u.getAsDouble() * 4), mul / 2);
        List<Integer> failures = new ArrayList<>();
        for (int i = 0; i < n; i++) failures.add((int) Math.round(u.getAsDouble() * 2 * (mul > 1 ? 1.5 : 1)));
        List<Double> energy = scale(series(n, () -> 900 + u.getAsDouble() * 400), mul / 2);
        List<Integer> maintenance = new ArrayList<>();
        for (int i = 0; i < n; i++) maintenance.add((int) Math.round((3 + u.getAsDouble() * 6) * (mul > 1 ? mul / 2 : 1)));

        List<Double> tCnc = trend(n, 56, 62, r);
        List<Double> tPress = trend(n, 70, 79, r);
        List<Double> tMotor = trend(n, 80, 91, r);
        List<Double> vCnc = trend(n, 2, 2.2, r);
        List<Double> vPress = trend(n, 4, 5.8, r);
        List<Double> vComp = trend(n, 2.6, 4.8, r);

        double oee = 80 + r.next() * 5;
        double mtbf = 380 + r.next() * 60;
        double mttr = 1.5 + r.next() * 0.8;
        long energyCost = Math.round(energy.stream().mapToDouble(Double::doubleValue).sum() * ENERGY_RATE_PER_KWH);
        long costAvoided = Math.round(planningDays * (9000 + r.next() * 4000));
        int planned = 88 + (int) Math.round(r.next() * 6);

        return new AnalyticsDto(
                labels, round1(uptime), round1(downtime), failures, round1(energy), maintenance,
                new AnalyticsDto.TempTrend(round1(tCnc), round1(tPress), round1(tMotor)),
                new AnalyticsDto.VibTrend(round1(vCnc), round1(vPress), round1(vComp)),
                new AnalyticsDto.Kpi(round1(oee), round1(mtbf), round1(mttr), energyCost, costAvoided, planned)
        );
    }

    private static List<Double> series(int n, DoubleSupplier gen) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(gen.getAsDouble());
        return out;
    }

    private static List<Double> scale(List<Double> values, double factor) {
        List<Double> out = new ArrayList<>();
        for (double v : values) out.add(v * factor);
        return out;
    }

    private static List<Double> trend(int n, double a, double b, MulberryRng r) {
        List<Double> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            double base = a + (b - a) * i / (double) (n - 1);
            out.add(base + (r.next() - 0.5) * (b - a) * 0.3);
        }
        return out;
    }

    private static List<Double> round1(List<Double> values) {
        List<Double> out = new ArrayList<>();
        for (double v : values) out.add(Math.round(v * 10) / 10.0);
        return out;
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
