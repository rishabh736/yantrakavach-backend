package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.*;
import com.yantrakavach.backend.domain.enums.*;
import com.yantrakavach.backend.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;

/**
 * Populates the database with a starter plant the first time the app runs (empty tables only — it never
 * overwrites data you've since changed). Machine names, zones, asset catalogue, technicians and the
 * starting alerts/work orders are ported from the frontend's own sample data generator
 * (src/lib/mock.ts) so a freshly connected frontend sees a familiar-looking plant on day one. Everything
 * here is ordinary starter content, not a real plant — replace it by editing or deleting rows once you
 * have real sensors reporting in.
 */
@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final long DAY = 86400L;

    private static final String[] NAMES = {
            "CNC Machine 01", "Hydraulic Press 02", "Motor Assembly 03", "Air Compressor 04", "Lathe Machine 05",
            "Conveyor Drive 06", "Injection Moulder 07", "Milling Machine 08", "CNC Machine 09", "Hydraulic Press 10",
            "Cooling Pump 11", "Grinding Machine 12", "Extruder Drive 13", "Lathe Machine 14", "Packaging Line 15",
            "Motor Assembly 16", "Air Compressor 17", "Drilling Machine 18", "CNC Machine 19", "Conveyor Drive 20",
            "Hydraulic Press 21", "Cooling Pump 22", "Milling Machine 23", "Welding Robot 24"
    };
    private static final String[] ZONES = {"Zone A · Machining", "Zone B · Pressing", "Zone C · Utilities", "Zone D · Assembly"};
    private static final Set<Integer> WARN_INDEX = Set.of(1, 3, 8, 12);
    private static final Set<Integer> CRIT_INDEX = Set.of(2, 17);

    private static final String T1 = "Ramesh Kumar";
    private static final String T2 = "Anita Sharma";
    private static final String T3 = "Vikram Singh";
    private static final String T4 = "Suresh Negi";

    private record Asset(String make, String model, double kw) {
    }

    private static final Map<String, Asset> ASSETS = Map.ofEntries(
            Map.entry("CNC Machine", new Asset("Arya Machine Tools", "VMC-640", 11)),
            Map.entry("Hydraulic Press", new Asset("Kaveri Hydraulics", "HP-100T", 15)),
            Map.entry("Motor Assembly", new Asset("Shivalik Drives", "IM-160L", 11)),
            Map.entry("Air Compressor", new Asset("Himalaya Pneumatics", "RC-7.5", 7.5)),
            Map.entry("Lathe Machine", new Asset("Arya Machine Tools", "CL-350", 7.5)),
            Map.entry("Conveyor Drive", new Asset("Shivalik Drives", "GD-90", 3.7)),
            Map.entry("Injection Moulder", new Asset("Ganga Plastech", "IM-150", 18.5)),
            Map.entry("Milling Machine", new Asset("Arya Machine Tools", "MM-4S", 5.5)),
            Map.entry("Cooling Pump", new Asset("Kaveri Hydraulics", "CP-50", 5.5)),
            Map.entry("Grinding Machine", new Asset("Arya Machine Tools", "SG-600", 4)),
            Map.entry("Extruder Drive", new Asset("Ganga Plastech", "EX-75", 22)),
            Map.entry("Packaging Line", new Asset("Doon Automation", "PL-2", 6)),
            Map.entry("Drilling Machine", new Asset("Arya Machine Tools", "RD-32", 2.2)),
            Map.entry("Welding Robot", new Asset("Doon Automation", "WR-6", 9))
    );

    private final MachineRepository machineRepository;
    private final SensorRepository sensorRepository;
    private final AlertRepository alertRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final ThresholdRepository thresholdRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.demo-password:demo123}")
    private String seedPassword;

    public DataSeeder(MachineRepository machineRepository, SensorRepository sensorRepository,
                       AlertRepository alertRepository, MaintenanceRepository maintenanceRepository,
                       ThresholdRepository thresholdRepository, UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.machineRepository = machineRepository;
        this.sensorRepository = sensorRepository;
        this.alertRepository = alertRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.thresholdRepository = thresholdRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedThresholds();
        seedUsers();
        List<Machine> machines = seedMachines();
        seedSensors(machines);
        seedAlerts();
        seedMaintenance();
    }

    private void seedThresholds() {
        if (thresholdRepository.count() > 0) return;
        thresholdRepository.saveAll(List.of(
                new ThresholdRow(ParamKey.VIBRATION, 4, 7),
                new ThresholdRow(ParamKey.TEMPERATURE, 70, 85),
                new ThresholdRow(ParamKey.SOUND, 80, 90),
                new ThresholdRow(ParamKey.CURRENT, 12, 15),
                new ThresholdRow(ParamKey.VOLTAGE, 5, 10)
        ));
        log.info("Seeded default thresholds");
    }

    private void seedUsers() {
        if (userRepository.count() > 0) return;
        String hash = passwordEncoder.encode(seedPassword);
        userRepository.saveAll(List.of(
                new AppUser("Plant Admin", "admin@yantrakavach.in", hash, Role.ADMIN),
                new AppUser("Maintenance Lead", "manager@yantrakavach.in", hash, Role.MAINTENANCE_MANAGER),
                new AppUser("Shift Operator", "operator@yantrakavach.in", hash, Role.OPERATOR)
        ));
        log.info("Seeded 3 starter accounts (password: {})", seedPassword);
    }

    private List<Machine> seedMachines() {
        if (machineRepository.count() > 0) return machineRepository.findAll();
        List<Machine> out = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < NAMES.length; i++) {
            MulberryRng r = new MulberryRng(i * 97 + 13);
            boolean warn = WARN_INDEX.contains(i);
            boolean crit = CRIT_INDEX.contains(i);
            Readings t = new Readings();
            if (i == 0) set(t, 61, 2.1, 68, 8.4, 414);
            else if (i == 1) set(t, 78, 5.8, 82, 12.7, 404);
            else if (i == 2) set(t, 91, 8.7, 92, 16.2, 388);
            else if (crit) set(t, 88 + r.next() * 3, 7.4 + r.next() * 1.2, 91 + r.next() * 2, 15.4 + r.next(), 392 + r.next() * 4);
            else if (warn) set(t, 71 + r.next() * 8, 4.3 + r.next() * 1.6, 81 + r.next() * 3, 12.1 + r.next() * 0.9, 403 + r.next() * 3);
            else set(t, 50 + r.next() * 15, 1.4 + r.next() * 1.6, 62 + r.next() * 10, 5 + r.next() * 6, 412 + r.next() * 6);

            boolean healthy = !warn && !crit;
            String name = NAMES[i];
            String type = name.replaceAll(" \\d+$", "");
            Asset asset = ASSETS.getOrDefault(type, new Asset("Arya Machine Tools", "GEN-1", 5));
            long lastMaintEpoch = now.getEpochSecond() - (6 + (long) Math.floor(r.next() * 70)) * DAY;

            Machine m = new Machine();
            m.setId(String.format("%02d", i + 1));
            m.setName(name);
            m.setType(type);
            m.setZone(ZONES[i / 6]);
            m.setMake(asset.make());
            m.setModel(asset.model());
            m.setSerial(asset.model() + "-" + (2016 + (int) Math.floor(r.next() * 8)) + "-" + (1000 + (int) Math.floor(r.next() * 8999)));
            m.setRatedKw(asset.kw());
            m.setInstalled(2016 + (int) Math.floor(r.next() * 9));
            m.setReadings(Readings.copyOf(t));
            m.setTarget(Readings.copyOf(t));

            Readings baseline = new Readings();
            set(baseline, 58, 2.2, 68, healthy ? t.getCurrent() : t.getCurrent() * 0.76, 415);
            m.setBaseline(baseline);

            m.setHistory(healthy ? 94 + Math.round(r.next() * 5) : warn ? 72 : 46);
            m.setOperatingHours(3200 + (int) Math.floor(r.next() * 9000));
            m.setUptime(healthy ? 97 + r.next() * 2.5 : warn ? 93 + r.next() * 3 : 88 + r.next() * 4);
            m.setLastMaintenance(Instant.ofEpochSecond(lastMaintEpoch));
            m.setNextMaintenance(Instant.ofEpochSecond(lastMaintEpoch + (healthy ? 90 : 45) * DAY));
            out.add(m);
        }
        machineRepository.saveAll(out);
        log.info("Seeded {} machines", out.size());
        return out;
    }

    private static void set(Readings r, double temp, double vib, double sound, double current, double voltage) {
        r.setTemperature(temp);
        r.setVibration(vib);
        r.setSound(sound);
        r.setCurrent(current);
        r.setVoltage(voltage);
    }

    private void seedSensors(List<Machine> machines) {
        if (sensorRepository.count() > 0) return;
        List<Sensor> out = new ArrayList<>();
        ParamKey[] params = ParamKey.values(); // VIBRATION, TEMPERATURE, SOUND, CURRENT, VOLTAGE
        Map<ParamKey, String> abbr = Map.of(
                ParamKey.VIBRATION, "VIB", ParamKey.TEMPERATURE, "TMP", ParamKey.SOUND, "SND",
                ParamKey.CURRENT, "CUR", ParamKey.VOLTAGE, "VLT");
        for (int i = 0; i < machines.size(); i++) {
            Machine m = machines.get(i);
            for (int k = 0; k < params.length; k++) {
                ParamKey p = params[k];
                MulberryRng r = new MulberryRng(i * 31 + k * 7 + 5);
                int battery = (int) Math.round(45 + r.next() * 55);
                int rssi = -(int) Math.round(44 + r.next() * 44);
                SensorState state = SensorState.ONLINE;
                if ((m.getId().equals("18") && p == ParamKey.SOUND) || (m.getId().equals("21") && p == ParamKey.VIBRATION)) {
                    state = SensorState.OFFLINE;
                    battery = 0;
                    rssi = -99;
                }
                if ((m.getId().equals("06") && p == ParamKey.CURRENT) || (m.getId().equals("15") && p == ParamKey.TEMPERATURE)) {
                    state = SensorState.LOW_BATTERY;
                    battery = 20;
                }
                String id = "VK-" + abbr.get(p) + "-" + String.format("%03d", i + 1);
                out.add(new Sensor(id, m.getId(), p, battery, rssi, state));
            }
        }
        sensorRepository.saveAll(out);
        log.info("Seeded {} sensors", out.size());
    }

    private void seedAlerts() {
        if (alertRepository.count() > 0) return;
        Instant now = Instant.now();
        List<Alert> out = new ArrayList<>();
        out.add(alert(1042, "03", Severity.CRITICAL, ParamKey.VIBRATION, now, 2, AlertStatus.OPEN, null, null));
        out.add(alert(1041, "02", Severity.WARNING, ParamKey.TEMPERATURE, now, 18, AlertStatus.OPEN, null, null));
        out.add(alert(1040, "18", Severity.CRITICAL, ParamKey.CURRENT, now, 34, AlertStatus.ACKNOWLEDGED, T2, null));
        out.add(alert(1039, "04", Severity.WARNING, ParamKey.VIBRATION, now, 55, AlertStatus.OPEN, null, null));
        out.add(alert(1038, "09", Severity.HIGH, ParamKey.CURRENT, now, 92, AlertStatus.OPEN, null, null));
        out.add(alert(1037, "01", Severity.INFO, ParamKey.TEMPERATURE, now, 300, AlertStatus.OPEN, null, "Scheduled lubrication due in 5 days."));
        out.add(alert(1036, "13", Severity.WARNING, ParamKey.SOUND, now, 700, AlertStatus.RESOLVED, T1, null));
        out.add(alert(1035, "12", Severity.HIGH, ParamKey.TEMPERATURE, now, 1500, AlertStatus.RESOLVED, T3, null));
        out.add(alert(1034, "06", Severity.INFO, ParamKey.CURRENT, now, 2400, AlertStatus.RESOLVED, T4, "Sensor VK-CUR-006 battery below 20%."));
        out.add(alert(1033, "10", Severity.WARNING, ParamKey.VIBRATION, now, 3100, AlertStatus.RESOLVED, T2, null));
        out.add(alert(1032, "16", Severity.WARNING, ParamKey.TEMPERATURE, now, 4300, AlertStatus.RESOLVED, T1, null));
        out.add(alert(1031, "07", Severity.HIGH, ParamKey.VIBRATION, now, 5900, AlertStatus.RESOLVED, T3, null));
        out.add(alert(1030, "21", Severity.INFO, ParamKey.VIBRATION, now, 7300, AlertStatus.RESOLVED, T4, "Sensor VK-VIB-021 lost connection to gateway."));
        out.add(alert(1029, "02", Severity.WARNING, ParamKey.VIBRATION, now, 8800, AlertStatus.RESOLVED, T2, null));
        alertRepository.saveAll(out);
        log.info("Seeded {} alerts", out.size());
    }

    private Alert alert(int id, String machineId, Severity severity, ParamKey param, Instant now, long minutesAgo,
                         AlertStatus status, String technician, String messageOverride) {
        Alert a = new Alert();
        a.setId("A-" + id);
        a.setMachineId(machineId);
        a.setSeverity(severity);
        a.setParam(param);
        a.setMessage(messageOverride != null ? messageOverride : DIAGNOSIS_ALERT.get(param));
        a.setCreatedAt(now.minusSeconds(minutesAgo * 60L));
        a.setStatus(status);
        a.setTechnician(technician);
        return a;
    }

    private static final Map<ParamKey, String> DIAGNOSIS_ALERT = Map.of(
            ParamKey.VIBRATION, "Abnormal vibration detected.",
            ParamKey.TEMPERATURE, "Temperature exceeding normal operating range.",
            ParamKey.CURRENT, "Motor current exceeding rated limit.",
            ParamKey.VOLTAGE, "Supply voltage outside tolerance.",
            ParamKey.SOUND, "Acoustic level above baseline."
    );

    private void seedMaintenance() {
        if (maintenanceRepository.count() > 0) return;
        Instant now = Instant.now();
        List<MaintenanceRecord> out = new ArrayList<>();
        int[] n = {2600};
        out.add(wo(n, "03", "Abnormal vibration", "Bearing replacement", T4, now, 0, 0, MaintenanceStatus.IN_PROGRESS, 8400, "6310 bearing ×2"));
        out.add(wo(n, "02", "Temperature above range", "Bearing assembly inspection", T2, now, -3, 0, MaintenanceStatus.SCHEDULED, 0, "—"));
        out.add(wo(n, "04", "Preventive service", "Lubrication and filter change", T1, now, -5, 0, MaintenanceStatus.SCHEDULED, 0, "Oil filter, compressor oil 5 L"));
        out.add(wo(n, "01", "Bearing inspection", "Lubrication performed", T1, now, 2, 42, MaintenanceStatus.COMPLETED, 650, "Grease cartridge"));
        out.add(wo(n, "12", "High spindle temperature", "Coolant line flushed, filter replaced", T3, now, 4, 95, MaintenanceStatus.COMPLETED, 3200, "Coolant filter, coolant 20 L"));
        out.add(wo(n, "13", "Acoustic anomaly", "Gear mesh inspected, fasteners torqued", T1, now, 6, 60, MaintenanceStatus.COMPLETED, 900, "—"));
        out.add(wo(n, "18", "Current spike on start-up", "Motor windings tested, contactor replaced", T2, now, 8, 180, MaintenanceStatus.COMPLETED, 5600, "Contactor 32 A"));
        out.add(wo(n, "02", "Hydraulic seal seepage", "Seal kit replacement", T4, now, 10, 150, MaintenanceStatus.COMPLETED, 7800, "Seal kit HP-100"));
        out.add(wo(n, "06", "Belt slippage", "Belt re-tensioned and aligned", T1, now, 13, 35, MaintenanceStatus.COMPLETED, 400, "V-belt B-68"));
        out.add(wo(n, "09", "Vibration above baseline", "Balancing and coupling check", T3, now, 17, 120, MaintenanceStatus.COMPLETED, 2900, "Coupling insert"));
        out.add(wo(n, "04", "Vibration trend review", "Mount bolts checked", T2, now, 21, 25, MaintenanceStatus.COMPLETED, 0, "—"));
        out.add(wo(n, "10", "Quarterly service", "Full mechanical inspection", T3, now, 28, 210, MaintenanceStatus.COMPLETED, 4100, "Filters, seals"));
        out.add(wo(n, "15", "Sensor battery replacement", "Batteries replaced on VK-TMP-015", T1, now, 31, 15, MaintenanceStatus.COMPLETED, 300, "Sensor batteries ×2"));
        out.add(wo(n, "07", "Hydraulic pressure drift", "Pump inspection", T4, now, 36, 90, MaintenanceStatus.COMPLETED, 6200, "Relief valve"));
        out.add(wo(n, "17", "Unusual compressor noise", "Valve plate replaced", T2, now, 41, 140, MaintenanceStatus.COMPLETED, 5200, "Valve plate kit"));
        out.add(wo(n, "11", "Seal leakage", "Mechanical seal replaced", T3, now, 47, 75, MaintenanceStatus.COMPLETED, 2800, "Mechanical seal"));
        out.add(wo(n, "19", "Spindle runout", "Spindle bearings re-preloaded", T1, now, 55, 240, MaintenanceStatus.COMPLETED, 9800, "Spindle bearing set"));
        out.add(wo(n, "24", "Wire feeder jam", "Feeder rollers replaced", T4, now, 63, 55, MaintenanceStatus.COMPLETED, 1500, "Feed rollers"));
        out.add(wo(n, "05", "Chuck wear", "Jaws re-ground", T2, now, 70, 120, MaintenanceStatus.COMPLETED, 1800, "—"));
        out.add(wo(n, "20", "Gearbox oil change", "Oil replaced, breather cleaned", T3, now, 78, 45, MaintenanceStatus.COMPLETED, 1200, "Gear oil 6 L"));
        maintenanceRepository.saveAll(out);
        log.info("Seeded {} maintenance records", out.size());
    }

    private MaintenanceRecord wo(int[] seq, String machineId, String issue, String action, String technician, Instant now,
                                  long daysAgo, int downtimeMin, MaintenanceStatus status, int cost, String parts) {
        MaintenanceRecord r = new MaintenanceRecord();
        r.setId("WO-" + (++seq[0]));
        r.setMachineId(machineId);
        r.setIssue(issue);
        r.setAction(action);
        r.setTechnician(technician);
        r.setDate(now.minusSeconds(daysAgo * DAY));
        r.setDowntimeMin(downtimeMin);
        r.setCost(cost);
        r.setParts(parts);
        r.setStatus(status);
        return r;
    }
}
