package com.yantrakavach.backend.domain;

import com.yantrakavach.backend.domain.enums.ParamKey;
import jakarta.persistence.Embeddable;

/** One reading (or target/baseline) for the five monitored parameters. Embedded three times per Machine. */
@Embeddable
public class Readings {

    private double temperature;
    private double vibration;
    private double sound;
    private double current;
    private double voltage;

    public Readings() {
    }

    public Readings(double temperature, double vibration, double sound, double current, double voltage) {
        this.temperature = temperature;
        this.vibration = vibration;
        this.sound = sound;
        this.current = current;
        this.voltage = voltage;
    }

    public static Readings copyOf(Readings r) {
        return new Readings(r.temperature, r.vibration, r.sound, r.current, r.voltage);
    }

    public double get(ParamKey key) {
        return switch (key) {
            case TEMPERATURE -> temperature;
            case VIBRATION -> vibration;
            case SOUND -> sound;
            case CURRENT -> current;
            case VOLTAGE -> voltage;
        };
    }

    public void set(ParamKey key, double value) {
        switch (key) {
            case TEMPERATURE -> temperature = value;
            case VIBRATION -> vibration = value;
            case SOUND -> sound = value;
            case CURRENT -> current = value;
            case VOLTAGE -> voltage = value;
        }
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public double getVibration() {
        return vibration;
    }

    public void setVibration(double vibration) {
        this.vibration = vibration;
    }

    public double getSound() {
        return sound;
    }

    public void setSound(double sound) {
        this.sound = sound;
    }

    public double getCurrent() {
        return current;
    }

    public void setCurrent(double current) {
        this.current = current;
    }

    public double getVoltage() {
        return voltage;
    }

    public void setVoltage(double voltage) {
        this.voltage = voltage;
    }
}
