package com.yantrakavach.backend.service;

import com.yantrakavach.backend.domain.Device;
import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.domain.Readings;
import com.yantrakavach.backend.domain.Sensor;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.SensorState;
import com.yantrakavach.backend.dto.DeviceCreateRequest;
import com.yantrakavach.backend.dto.DeviceCreatedDto;
import com.yantrakavach.backend.dto.DeviceDto;
import com.yantrakavach.backend.dto.IngestRequest;
import com.yantrakavach.backend.dto.IngestResponse;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.repo.DeviceRepository;
import com.yantrakavach.backend.repo.MachineRepository;
import com.yantrakavach.backend.repo.SensorRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provisions devices (an Admin/Manager action, done once per ESP32) and handles the readings those
 * devices post afterwards (an unauthenticated-by-login, API-key-authenticated action, done continuously).
 */
@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);

    private static final Map<ParamKey, String> ABBR = Map.of(
            ParamKey.VIBRATION, "VIB", ParamKey.TEMPERATURE, "TMP", ParamKey.SOUND, "SND",
            ParamKey.CURRENT, "CUR", ParamKey.VOLTAGE, "VLT");

    private final DeviceRepository deviceRepository;
    private final MachineRepository machineRepository;
    private final SensorRepository sensorRepository;
    private final TelemetryService telemetryService;

    @Value("${app.simulation.device-timeout-seconds:60}")
    private long deviceTimeoutSeconds;

    public DeviceService(DeviceRepository deviceRepository, MachineRepository machineRepository,
                          SensorRepository sensorRepository, TelemetryService telemetryService) {
        this.deviceRepository = deviceRepository;
        this.machineRepository = machineRepository;
        this.sensorRepository = sensorRepository;
        this.telemetryService = telemetryService;
    }

    @Transactional
    public DeviceCreatedDto provision(DeviceCreateRequest req) {
        if (!machineRepository.existsById(req.getMachineId())) {
            throw ApiException.badRequest("No machine with id " + req.getMachineId());
        }
        if (deviceRepository.existsById(req.getDeviceId())) {
            throw ApiException.badRequest("Device id " + req.getDeviceId() + " is already registered");
        }
        String raw = DeviceKeys.generateRaw();
        String label = (req.getLabel() == null || req.getLabel().isBlank())
                ? ("Sensor gateway for machine " + req.getMachineId())
                : req.getLabel();
        Device device = new Device(req.getDeviceId(), req.getMachineId(), label, DeviceKeys.sha256Hex(raw));
        deviceRepository.save(device);
        return new DeviceCreatedDto(device.getId(), device.getMachineId(), device.getLabel(), raw);
    }

    public List<DeviceDto> list() {
        Instant cutoff = Instant.now().minusSeconds(deviceTimeoutSeconds);
        return deviceRepository.findAll().stream()
                .map(d -> new DeviceDto(d.getId(), d.getMachineId(), d.getLabel(),
                        d.getLastSeenAt() != null && d.getLastSeenAt().isAfter(cutoff), d.getLastSeenAt()))
                .toList();
    }

    @Transactional
    public void revoke(String deviceId) {
        if (!deviceRepository.existsById(deviceId)) {
            throw ApiException.notFound("No device with id " + deviceId);
        }
        deviceRepository.deleteById(deviceId);
    }

    @Transactional
    public IngestResponse ingest(String rawKey, IngestRequest body) {
        if (rawKey == null || rawKey.isBlank()) {
            throw ApiException.unauthorized("Missing X-Device-Key header.");
        }
        Device device = deviceRepository.findByApiKeyHash(DeviceKeys.sha256Hex(rawKey))
                .orElseThrow(() -> ApiException.unauthorized("Unknown or revoked device key."));
        Machine machine = machineRepository.findById(device.getMachineId())
                .orElseThrow(() -> ApiException.notFound("Device " + device.getId() + " is registered to a machine that no longer exists."));

        if (body.getMachineId() != null && !body.getMachineId().isBlank() && !body.getMachineId().equals(device.getMachineId())) {
            // Informational only — see IngestRequest's class comment on why the body never controls routing.
            log.debug("Device {} sent machine_id '{}' in its body, but is registered to internal machine {}; "
                    + "applying readings to {} as provisioned.", device.getId(), body.getMachineId(), device.getMachineId(), device.getMachineId());
        }

        Readings readings = machine.getReadings();
        Readings target = machine.getTarget();
        boolean any = false;
        any |= apply(ParamKey.TEMPERATURE, body.getTemperature(), readings, target, machine, body.getBattery(), body.getRssi());
        any |= apply(ParamKey.VIBRATION, body.getVibration(), readings, target, machine, body.getBattery(), body.getRssi());
        any |= apply(ParamKey.SOUND, body.getSound(), readings, target, machine, body.getBattery(), body.getRssi());
        any |= apply(ParamKey.CURRENT, body.getCurrent(), readings, target, machine, body.getBattery(), body.getRssi());
        any |= apply(ParamKey.VOLTAGE, body.getVoltage(), readings, target, machine, body.getBattery(), body.getRssi());

        // Extra telemetry outside the five-parameter health model: stored on the machine, but doesn't
        // feed the health score or threshold checks, and alone doesn't mark any Sensor row online.
        if (body.getPowerW() != null) {
            machine.setPowerW(body.getPowerW());
            any = true;
        }
        if (body.getRpm() != null) {
            machine.setRpm(body.getRpm());
            any = true;
        }
        if (body.getSensorStatus() != null && !body.getSensorStatus().isBlank()) {
            machine.setSensorStatus(body.getSensorStatus());
            any = true;
        }
        if (body.getTimestamp() != null && !body.getTimestamp().isBlank()) {
            machine.setLastDeviceTimestamp(body.getTimestamp());
        }

        if (!any) {
            throw ApiException.badRequest("Body had no readings — include at least one of temperature, vibration, sound, current, voltage, power_w, rpm or sensor_status.");
        }

        Instant now = Instant.now();
        device.setLastSeenAt(now);
        deviceRepository.save(device);
        telemetryService.applyAndCheck(machine); // also saves the machine

        return new IngestResponse(true, machine.getId(), now);
    }

    /** Writes one parameter into readings/target and marks its Sensor row online, if that field was sent. */
    private boolean apply(ParamKey key, Double value, Readings readings, Readings target, Machine machine,
                           Integer battery, Integer rssi) {
        if (value == null) return false;
        readings.set(key, value);
        target.set(key, value); // so a later resumed simulation drifts from the real last value, not a stale target
        touchSensor(machine.getId(), key, battery, rssi);
        return true;
    }

    private void touchSensor(String machineId, ParamKey param, Integer battery, Integer rssi) {
        int index;
        try {
            index = Integer.parseInt(machineId);
        } catch (NumberFormatException e) {
            return; // non-numeric machine id — skip sensor bookkeeping rather than guess an id
        }
        String id = "VK-" + ABBR.get(param) + "-" + String.format("%03d", index);
        Sensor sensor = sensorRepository.findById(id).orElseGet(() -> new Sensor(id, machineId, param, 100, -50, SensorState.ONLINE));
        sensor.setMachineId(machineId);
        sensor.setParam(param);
        sensor.setState(SensorState.ONLINE);
        if (battery != null) sensor.setBattery(Math.max(0, Math.min(100, battery)));
        if (rssi != null) sensor.setRssi(rssi);
        sensorRepository.save(sensor);
    }
}
