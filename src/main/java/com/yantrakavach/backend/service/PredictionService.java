package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.Status;
import com.yantrakavach.backend.dto.PredictionDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds an explainable prediction (what / where / why / impact / action + confidence) for a machine.
 * Ported from the frontend's predict() in src/lib/engine.ts so the explanation reads the same way
 * whichever side computed it.
 */
@Service
public class PredictionService {

    private final ThresholdService thresholds;
    private final DiagnosisCatalog diagnosis;

    public PredictionService(ThresholdService thresholds, DiagnosisCatalog diagnosis) {
        this.thresholds = thresholds;
        this.diagnosis = diagnosis;
    }

    public PredictionDto predict(Machine m) {
        Status status = thresholds.machineStatus(m.getReadings());
        int health = thresholds.healthScore(m);
        ParamKey worst = thresholds.worstParam(m);
        DiagnosisCatalog.Entry d = diagnosis.get(worst);

        List<ParamKey> abnormal = new ArrayList<>();
        for (ParamKey key : ParamKey.values()) {
            if (thresholds.paramStatus(key, m.getReadings().get(key)) != Status.HEALTHY) abnormal.add(key);
        }

        int pct = worst == ParamKey.VOLTAGE
                ? (int) Math.round(thresholds.deviation(ParamKey.VOLTAGE, m.getReadings().getVoltage()))
                : (int) Math.round((m.getReadings().get(worst) / m.getBaseline().get(worst) - 1) * 100);

        boolean correlated = abnormal.contains(ParamKey.VIBRATION) && abnormal.contains(ParamKey.TEMPERATURE);

        String risk = switch (status) {
            case CRITICAL -> "High";
            case WARNING -> "Medium";
            case HEALTHY -> "Low";
        };
        String window = switch (status) {
            case CRITICAL -> "1–3 days";
            case WARNING -> "7–14 days";
            case HEALTHY -> "More than 60 days";
        };
        String pattern = status == Status.HEALTHY
                ? "No abnormal pattern"
                : correlated ? "Increasing vibration + temperature correlation" : (label(worst) + " drifting above baseline");
        String why = worst == ParamKey.VOLTAGE
                ? ("Supply voltage deviates " + pct + "% from the " + (int) ThresholdService.NOMINAL_VOLTAGE + " V nominal.")
                : (label(worst) + " amplitude has increased by " + pct + "% over the baseline.");

        int hash = Math.floorMod(MulberryRng.hashStr(m.getId()), 6);
        int confidence = Math.min(96, 70 + Math.round((100 - health) / 3f) + hash);
        int stage = switch (status) {
            case CRITICAL -> 4;
            case WARNING -> health < 70 ? 3 : 2;
            case HEALTHY -> 0;
        };

        return new PredictionDto(m, status, health, worst, abnormal, risk, window, pattern,
                d.what(), d.where(), why, d.impact(), d.action(), confidence, stage);
    }

    private static String label(ParamKey key) {
        return switch (key) {
            case VIBRATION -> "Vibration";
            case TEMPERATURE -> "Temperature";
            case SOUND -> "Sound";
            case CURRENT -> "Current";
            case VOLTAGE -> "Voltage";
        };
    }
}
