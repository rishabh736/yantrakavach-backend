package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.enums.ParamKey;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

/**
 * The plain-language explanation shown for each parameter when it is abnormal — ported from the
 * frontend's DIAGNOSIS map in src/lib/engine.ts so the backend's /api/predictions reads the same way.
 */
@Component
public class DiagnosisCatalog {

    public record Entry(String what, String where, String impact, String action, String alertMessage) {
    }

    private final Map<ParamKey, Entry> entries = new EnumMap<>(ParamKey.class);

    public DiagnosisCatalog() {
        entries.put(ParamKey.VIBRATION, new Entry(
                "Abnormal vibration detected.",
                "Drive-end bearing region.",
                "Potential bearing degradation.",
                "Inspect bearing lubrication and alignment.",
                "Abnormal vibration detected."));
        entries.put(ParamKey.TEMPERATURE, new Entry(
                "Sustained overheating detected.",
                "Motor housing and gearbox surface.",
                "Insulation ageing and lubricant breakdown.",
                "Check cooling airflow, lubricant level and load.",
                "Temperature exceeding normal operating range."));
        entries.put(ParamKey.CURRENT, new Entry(
                "Motor current above rated load.",
                "Main drive motor winding.",
                "Winding stress and possible mechanical binding.",
                "Inspect belt tension, coupling and load condition.",
                "Motor current exceeding rated limit."));
        entries.put(ParamKey.VOLTAGE, new Entry(
                "Supply voltage outside tolerance.",
                "Incoming three-phase supply.",
                "Motor overheating and controller faults.",
                "Check phase balance and terminal connections.",
                "Supply voltage outside tolerance."));
        entries.put(ParamKey.SOUND, new Entry(
                "Acoustic level above baseline.",
                "Gearbox and spindle assembly.",
                "Gear wear or loose components.",
                "Inspect gears and fasteners for wear.",
                "Acoustic level above baseline."));
    }

    public Entry get(ParamKey key) {
        return entries.get(key);
    }
}
