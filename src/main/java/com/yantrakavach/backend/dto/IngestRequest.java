package com.yantrakavach.backend.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * Body an ESP32 (or any other sensor gateway) posts to /api/ingest/readings. Every reading field is
 * optional: send only the parameters you actually have a sensor wired up for, and omit (or send null
 * for) the rest — they will simply not be touched.
 *
 * <p>Two naming schemes are accepted for the same five readings, via @JsonAlias, so you can use whichever
 * reads more naturally in your firmware:
 * <pre>
 *   temperature | temperature_c      vibration | vibration_rms      sound | sound_level
 *   current     | current_a          voltage   | voltage_v
 * </pre>
 *
 * <p>machine_id and timestamp are accepted but are <b>informational only</b> — which machine gets updated
 * is always decided by the X-Device-Key header, never by anything in the body. That's deliberate: if the
 * body could redirect readings to any machine, one leaked device key would let someone overwrite any
 * machine's data, not just the one it was provisioned for. Put whatever your firmware calls itself in
 * machine_id if that's useful for your own logging — the backend logs it but never acts on it.
 *
 * <p>power_w, rpm and sensor_status aren't part of the five-parameter health-scoring model (there's no
 * threshold or health-score weighting for them), but are stored on the machine as extra telemetry so nothing
 * you send is silently discarded.
 */
public class IngestRequest {

    @JsonAlias("temperature_c")
    private Double temperature;

    @JsonAlias("vibration_rms")
    private Double vibration;

    @JsonAlias("sound_level")
    private Double sound;

    @JsonAlias("current_a")
    private Double current;

    @JsonAlias("voltage_v")
    private Double voltage;

    private Integer battery;
    private Integer rssi;

    /** Informational only — see class comment. */
    @JsonAlias("machine_id")
    private String machineId;

    /** The device's own idea of the time (ISO-8601), stored as-is. Not used for staleness — the backend
     *  uses its own clock for that, since a cheap ESP32 RTC can't be trusted to stay accurate. */
    private String timestamp;

    @JsonAlias("power_w")
    private Double powerW;

    private Integer rpm;

    @JsonAlias("sensor_status")
    private String sensorStatus;

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getVibration() {
        return vibration;
    }

    public void setVibration(Double vibration) {
        this.vibration = vibration;
    }

    public Double getSound() {
        return sound;
    }

    public void setSound(Double sound) {
        this.sound = sound;
    }

    public Double getCurrent() {
        return current;
    }

    public void setCurrent(Double current) {
        this.current = current;
    }

    public Double getVoltage() {
        return voltage;
    }

    public void setVoltage(Double voltage) {
        this.voltage = voltage;
    }

    public Integer getBattery() {
        return battery;
    }

    public void setBattery(Integer battery) {
        this.battery = battery;
    }

    public Integer getRssi() {
        return rssi;
    }

    public void setRssi(Integer rssi) {
        this.rssi = rssi;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public Double getPowerW() {
        return powerW;
    }

    public void setPowerW(Double powerW) {
        this.powerW = powerW;
    }

    public Integer getRpm() {
        return rpm;
    }

    public void setRpm(Integer rpm) {
        this.rpm = rpm;
    }

    public String getSensorStatus() {
        return sensorStatus;
    }

    public void setSensorStatus(String sensorStatus) {
        this.sensorStatus = sensorStatus;
    }
}
