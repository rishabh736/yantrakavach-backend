package com.yantrakavach.backend.domain;

import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.SensorState;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One physical sensor, e.g. VK-VIB-001, reading one parameter on one machine. */
@Entity
@Table(name = "sensors")
public class Sensor {

    @Id
    private String id; // "VK-VIB-001"

    private String machineId;

    @Enumerated(EnumType.STRING)
    private ParamKey param;

    private int battery; // 0-100
    private int rssi;    // dBm, negative

    @Enumerated(EnumType.STRING)
    private SensorState state;

    public Sensor() {
    }

    public Sensor(String id, String machineId, ParamKey param, int battery, int rssi, SensorState state) {
        this.id = id;
        this.machineId = machineId;
        this.param = param;
        this.battery = battery;
        this.rssi = rssi;
        this.state = state;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMachineId() {
        return machineId;
    }

    public void setMachineId(String machineId) {
        this.machineId = machineId;
    }

    public ParamKey getParam() {
        return param;
    }

    public void setParam(ParamKey param) {
        this.param = param;
    }

    public int getBattery() {
        return battery;
    }

    public void setBattery(int battery) {
        this.battery = battery;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public SensorState getState() {
        return state;
    }

    public void setState(SensorState state) {
        this.state = state;
    }
}
