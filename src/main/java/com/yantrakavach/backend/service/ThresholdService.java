package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.domain.Readings;
import com.yantrakavach.backend.domain.ThresholdRow;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.Status;
import com.yantrakavach.backend.dto.ThresholdPairDto;
import com.yantrakavach.backend.repo.ThresholdRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the live warning/critical thresholds and the health-scoring engine. This is a line-for-line port
 * of the frontend's src/lib/engine.ts so a machine judged "critical" here is judged "critical" there too.
 */
@Service
public class ThresholdService {

    public static final double NOMINAL_VOLTAGE = 415.0;

    /** vibration, temperature, current, voltage, sound, history — must sum to 1.0 (matches engine.ts WEIGHTS). */
    private static final Map<String, Double> WEIGHTS = Map.of(
            "vibration", 0.30,
            "temperature", 0.25,
            "current", 0.15,
            "voltage", 0.10,
            "sound", 0.10,
            "history", 0.10
    );

    private final ThresholdRepository repository;
    private final Map<ParamKey, double[]> live = new ConcurrentHashMap<>(); // [warn, crit]

    public ThresholdService(ThresholdRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void load() {
        for (ThresholdRow row : repository.findAll()) {
            live.put(row.getParam(), new double[]{row.getWarn(), row.getCrit()});
        }
    }

    /**
     * Returns thresholds keyed by lowercase parameter name ("vibration", "temperature", ...), matching the
     * frontend's Thresholds type exactly. Built as a String-keyed map on purpose: Jackson does not reliably
     * honour an enum's @JsonValue for *map keys* (only for values), so returning Map<ParamKey, ...> directly
     * risks serializing keys as "VIBRATION" instead of "vibration" depending on the Jackson version in play.
     */
    public Map<String, ThresholdPairDto> current() {
        Map<String, ThresholdPairDto> out = new LinkedHashMap<>();
        for (ParamKey key : ParamKey.values()) {
            double[] t = live.get(key);
            out.put(key.toJson(), new ThresholdPairDto(t[0], t[1]));
        }
        return out;
    }

    public void update(Map<String, ThresholdPairDto> next) {
        for (Map.Entry<String, ThresholdPairDto> e : next.entrySet()) {
            if (e.getValue() == null) continue;
            ParamKey key;
            try {
                key = ParamKey.fromJson(e.getKey());
            } catch (IllegalArgumentException ex) {
                continue; // unknown key in the request body — ignore rather than fail the whole update
            }
            double warn = e.getValue().warn();
            double crit = e.getValue().crit();
            if (warn <= 0 || crit <= 0) continue;
            live.put(key, new double[]{warn, crit});
            ThresholdRow row = repository.findById(key).orElse(new ThresholdRow(key, warn, crit));
            row.setWarn(warn);
            row.setCrit(crit);
            repository.save(row);
        }
    }

    /** Voltage is judged as % deviation from nominal; every other parameter is judged on its raw value. */
    public double deviation(ParamKey key, double value) {
        if (key == ParamKey.VOLTAGE) {
            return Math.abs(value - NOMINAL_VOLTAGE) / NOMINAL_VOLTAGE * 100.0;
        }
        return value;
    }

    public Status paramStatus(ParamKey key, double value) {
        double[] t = live.get(key);
        double x = deviation(key, value);
        if (x > t[1]) return Status.CRITICAL;
        if (x >= t[0]) return Status.WARNING;
        return Status.HEALTHY;
    }

    public Status machineStatus(Readings readings) {
        Status worst = Status.HEALTHY;
        for (ParamKey key : ParamKey.values()) {
            Status s = paramStatus(key, readings.get(key));
            if (s.ordinal() > worst.ordinal()) worst = s;
        }
        return worst;
    }

    public double factorScore(ParamKey key, double value) {
        double[] t = live.get(key);
        double warn = t[0];
        double crit = t[1];
        double x = deviation(key, value);
        double ok = warn * 0.8;
        if (x <= ok) return 100;
        if (x <= warn) return 100 - ((x - ok) / (warn - ok)) * 30;
        if (x <= crit) return 70 - ((x - warn) / (crit - warn)) * 40;
        return Math.max(5, 30 - ((x - crit) / crit) * 60);
    }

    public Map<String, Double> factorScores(Machine m) {
        Map<String, Double> f = new LinkedHashMap<>();
        f.put("vibration", factorScore(ParamKey.VIBRATION, m.getReadings().getVibration()));
        f.put("temperature", factorScore(ParamKey.TEMPERATURE, m.getReadings().getTemperature()));
        f.put("current", factorScore(ParamKey.CURRENT, m.getReadings().getCurrent()));
        f.put("voltage", factorScore(ParamKey.VOLTAGE, m.getReadings().getVoltage()));
        f.put("sound", factorScore(ParamKey.SOUND, m.getReadings().getSound()));
        f.put("history", m.getHistory());
        return f;
    }

    public int healthScore(Machine m) {
        Map<String, Double> f = factorScores(m);
        double total = 0;
        for (Map.Entry<String, Double> e : WEIGHTS.entrySet()) {
            total += f.get(e.getKey()) * e.getValue();
        }
        return (int) Math.round(total);
    }

    private double ratio(Machine m, ParamKey key) {
        double[] t = live.get(key);
        return deviation(key, m.getReadings().get(key)) / t[0];
    }

    public ParamKey worstParam(Machine m) {
        ParamKey worst = ParamKey.VIBRATION;
        for (ParamKey key : ParamKey.values()) {
            if (ratio(m, key) > ratio(m, worst)) worst = key;
        }
        return worst;
    }

    public Map<ParamKey, double[]> snapshot() {
        return new EnumMap<>(live);
    }
}
