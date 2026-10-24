package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.Alert;
import com.yantrakavach.backend.domain.Device;
import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.domain.Readings;
import com.yantrakavach.backend.domain.Sensor;
import com.yantrakavach.backend.domain.enums.AlertStatus;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.SensorState;
import com.yantrakavach.backend.domain.enums.Severity;
import com.yantrakavach.backend.domain.enums.Status;
import com.yantrakavach.backend.repo.AlertRepository;
import com.yantrakavach.backend.repo.DeviceRepository;
import com.yantrakavach.backend.repo.MachineRepository;
import com.yantrakavach.backend.repo.SensorRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

/**
 * Two jobs, both on a timer:
 *
 * <p>1. For any machine that is NOT currently reporting from a real device (see Device / DeviceService),
 * nudge its readings toward its target, exactly like the frontend's own PlantContext.tsx simulation does
 * — so a machine still looks "alive" in the demo even with no hardware attached to it yet.
 *
 * <p>2. For any machine that a device HAS reported for recently, leave its readings alone (the real data
 * from {@link DeviceService#ingest} already holds them) and instead watch for that device going quiet,
 * flipping its sensors to offline if it has.
 *
 * <p>Both paths funnel status changes through {@link #applyAndCheck}, so "a reading crossed a threshold"
 * raises exactly one alert and updates exactly one status history, whether the reading came from the
 * simulator or from a real ESP32.
 */
@Service
public class TelemetryService {

    private static final Logger log = LoggerFactory.getLogger(TelemetryService.class);
    private static final Pattern ALERT_NUM = Pattern.compile("A-(\\d+)");

    /** How much a reading can jitter per tick, matching src/lib/params.ts PARAMS[*].noise. */
    private static final Map<ParamKey, Double> NOISE = new EnumMap<>(Map.of(
            ParamKey.VIBRATION, 0.1,
            ParamKey.TEMPERATURE, 0.45,
            ParamKey.SOUND, 0.7,
            ParamKey.CURRENT, 0.1,
            ParamKey.VOLTAGE, 1.0
    ));

    private final MachineRepository machineRepository;
    private final AlertRepository alertRepository;
    private final SensorRepository sensorRepository;
    private final DeviceRepository deviceRepository;
    private final ThresholdService thresholds;
    private final DiagnosisCatalog diagnosis;

    private final Map<String, Status> lastStatus = new ConcurrentHashMap<>();
    private final AtomicInteger alertSeq = new AtomicInteger(1042);

    @Value("${app.simulation.device-timeout-seconds:60}")
    private long deviceTimeoutSeconds;

    public TelemetryService(MachineRepository machineRepository, AlertRepository alertRepository,
                             SensorRepository sensorRepository, DeviceRepository deviceRepository,
                             ThresholdService thresholds, DiagnosisCatalog diagnosis) {
        this.machineRepository = machineRepository;
        this.alertRepository = alertRepository;
        this.sensorRepository = sensorRepository;
        this.deviceRepository = deviceRepository;
        this.thresholds = thresholds;
        this.diagnosis = diagnosis;
    }

    @PostConstruct
    public void init() {
        for (Alert a : alertRepository.findAll()) {
            var m = ALERT_NUM.matcher(a.getId());
            if (m.matches()) alertSeq.updateAndGet(cur -> Math.max(cur, Integer.parseInt(m.group(1))));
        }
        for (Machine machine : machineRepository.findAll()) {
            lastStatus.put(machine.getId(), thresholds.machineStatus(machine.getReadings()));
        }
        log.info("Telemetry simulation ready for {} machines", lastStatus.size());
    }

    /** Machine ids that a real device has reported for within the configured timeout. */
    private Set<String> liveMachineIds() {
        Instant cutoff = Instant.now().minusSeconds(deviceTimeoutSeconds);
        Set<String> live = new HashSet<>();
        for (Device d : deviceRepository.findAll()) {
            if (d.getLastSeenAt() != null && d.getLastSeenAt().isAfter(cutoff)) {
                live.add(d.getMachineId());
            }
        }
        return live;
    }

    @Scheduled(fixedDelayString = "${app.simulation.tick-ms:1500}")
    @Transactional
    public void tick() {
        Set<String> live = liveMachineIds();
        List<Machine> machines = machineRepository.findAll();
        for (Machine machine : machines) {
            if (live.contains(machine.getId())) continue; // a real device owns this machine's readings right now
            Readings r = machine.getReadings();
            Readings t = machine.getTarget();
            for (ParamKey key : ParamKey.values()) {
                double current = r.get(key);
                double target = t.get(key);
                double noise = NOISE.get(key);
                double next = current + (target - current) * 0.25 + (ThreadLocalRandom.current().nextDouble() - 0.5) * noise;
                r.set(key, Math.max(0.1, next));
            }
            applyAndCheck(machine);
        }
    }

    /**
     * Compares a machine's current readings against its last known status, raises a new alert if things
     * got worse, remembers the new status, and persists the machine. Called once per tick for simulated
     * machines, and once per request for machines fed by a real device (see DeviceService#ingest).
     */
    @Transactional
    public void applyAndCheck(Machine machine) {
        Status status = thresholds.machineStatus(machine.getReadings());
        Status previous = lastStatus.getOrDefault(machine.getId(), Status.HEALTHY);
        if (status.ordinal() > previous.ordinal()) {
            raiseAlert(machine, status);
        }
        lastStatus.put(machine.getId(), status);
        machineRepository.save(machine);
    }

    private void raiseAlert(Machine machine, Status status) {
        ParamKey worst = thresholds.worstParam(machine);
        int health = thresholds.healthScore(machine);
        Severity severity = status == Status.CRITICAL ? Severity.CRITICAL : (health < 60 ? Severity.HIGH : Severity.WARNING);

        Alert alert = new Alert();
        alert.setId("A-" + alertSeq.incrementAndGet());
        alert.setMachineId(machine.getId());
        alert.setSeverity(severity);
        alert.setParam(worst);
        alert.setMessage(diagnosis.get(worst).alertMessage());
        alert.setCreatedAt(Instant.now());
        alert.setStatus(AlertStatus.OPEN);
        alertRepository.save(alert);
        log.info("Raised {} alert {} on {} ({})", severity, alert.getId(), machine.getName(), worst);
    }

    /** Flips a machine's sensors offline once its device has gone quiet for longer than the timeout. */
    @Scheduled(fixedDelay = 15000)
    @Transactional
    public void sweepStaleDevices() {
        Instant cutoff = Instant.now().minusSeconds(deviceTimeoutSeconds);
        for (Device device : deviceRepository.findAll()) {
            boolean stale = device.getLastSeenAt() == null || device.getLastSeenAt().isBefore(cutoff);
            if (!stale) continue;
            List<Sensor> sensors = sensorRepository.findByMachineId(device.getMachineId());
            boolean changed = false;
            for (Sensor s : sensors) {
                if (s.getState() != SensorState.OFFLINE) {
                    s.setState(SensorState.OFFLINE);
                    s.setRssi(-99);
                    changed = true;
                }
            }
            if (changed) {
                sensorRepository.saveAll(sensors);
                log.info("Device {} went quiet — marked its sensors on machine {} offline", device.getId(), device.getMachineId());
            }
        }
    }

    /**
     * Pushes a machine's target so the next few ticks drift it into a fault state — the server-side
     * equivalent of the frontend's "Simulate fault" demo button. Has no effect on a machine currently
     * fed by a real device, since its readings aren't touched by tick() at all.
     */
    public void injectFault(String machineId) {
        machineRepository.findById(machineId).ifPresent(m -> {
            Readings t = m.getTarget();
            t.setVibration(8.3);
            t.setTemperature(87);
            t.setSound(91);
            machineRepository.save(m);
        });
    }

    public void resetTarget(String machineId, Readings originalTarget) {
        machineRepository.findById(machineId).ifPresent(m -> {
            m.setTarget(Readings.copyOf(originalTarget));
            machineRepository.save(m);
        });
    }
}
