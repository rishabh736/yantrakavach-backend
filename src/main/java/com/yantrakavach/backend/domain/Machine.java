package com.yantrakavach.backend.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * A monitored machine. Field names match the frontend's Machine interface (src/types.ts) so this
 * entity can be serialized straight to JSON without a separate DTO.
 */
@Entity
@Table(name = "machines")
public class Machine {

    @Id
    private String id; // "01".."24"

    private String name;
    private String type;
    private String zone;

    private String make;
    private String model;
    private String serial;
    private double ratedKw;
    private int installed;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "temperature", column = @Column(name = "r_temperature")),
            @AttributeOverride(name = "vibration", column = @Column(name = "r_vibration")),
            @AttributeOverride(name = "sound", column = @Column(name = "r_sound")),
            @AttributeOverride(name = "current", column = @Column(name = "r_current")),
            @AttributeOverride(name = "voltage", column = @Column(name = "r_voltage"))
    })
    private Readings readings = new Readings();

    /** The value the simulated telemetry drifts toward. Not shown in the UI directly, but kept for shape parity. */
    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "temperature", column = @Column(name = "t_temperature")),
            @AttributeOverride(name = "vibration", column = @Column(name = "t_vibration")),
            @AttributeOverride(name = "sound", column = @Column(name = "t_sound")),
            @AttributeOverride(name = "current", column = @Column(name = "t_current")),
            @AttributeOverride(name = "voltage", column = @Column(name = "t_voltage"))
    })
    private Readings target = new Readings();

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "temperature", column = @Column(name = "b_temperature")),
            @AttributeOverride(name = "vibration", column = @Column(name = "b_vibration")),
            @AttributeOverride(name = "sound", column = @Column(name = "b_sound")),
            @AttributeOverride(name = "current", column = @Column(name = "b_current")),
            @AttributeOverride(name = "voltage", column = @Column(name = "b_voltage"))
    })
    private Readings baseline = new Readings();

    /** 0-100 score representing how clean the machine's maintenance history is; one factor in the health score. */
    private double history;

    private int operatingHours;
    private double uptime;

    private Instant lastMaintenance;
    private Instant nextMaintenance;

    // --- Extra telemetry some devices report that isn't part of the five-parameter health model.
    // All nullable: plenty of machines (and the seeded demo data) will simply never have these set.
    private Double powerW;
    private Integer rpm;
    private String sensorStatus;
    /** The reporting device's own clock, as a raw ISO-8601 string. Not used for staleness — see IngestRequest. */
    private String lastDeviceTimestamp;

    public Machine() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getZone() {
        return zone;
    }

    public void setZone(String zone) {
        this.zone = zone;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }

    public double getRatedKw() {
        return ratedKw;
    }

    public void setRatedKw(double ratedKw) {
        this.ratedKw = ratedKw;
    }

    public int getInstalled() {
        return installed;
    }

    public void setInstalled(int installed) {
        this.installed = installed;
    }

    public Readings getReadings() {
        return readings;
    }

    public void setReadings(Readings readings) {
        this.readings = readings;
    }

    public Readings getTarget() {
        return target;
    }

    public void setTarget(Readings target) {
        this.target = target;
    }

    public Readings getBaseline() {
        return baseline;
    }

    public void setBaseline(Readings baseline) {
        this.baseline = baseline;
    }

    public double getHistory() {
        return history;
    }

    public void setHistory(double history) {
        this.history = history;
    }

    public int getOperatingHours() {
        return operatingHours;
    }

    public void setOperatingHours(int operatingHours) {
        this.operatingHours = operatingHours;
    }

    public double getUptime() {
        return uptime;
    }

    public void setUptime(double uptime) {
        this.uptime = uptime;
    }

    public Instant getLastMaintenance() {
        return lastMaintenance;
    }

    public void setLastMaintenance(Instant lastMaintenance) {
        this.lastMaintenance = lastMaintenance;
    }

    public Instant getNextMaintenance() {
        return nextMaintenance;
    }

    public void setNextMaintenance(Instant nextMaintenance) {
        this.nextMaintenance = nextMaintenance;
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

    public String getLastDeviceTimestamp() {
        return lastDeviceTimestamp;
    }

    public void setLastDeviceTimestamp(String lastDeviceTimestamp) {
        this.lastDeviceTimestamp = lastDeviceTimestamp;
    }
}
