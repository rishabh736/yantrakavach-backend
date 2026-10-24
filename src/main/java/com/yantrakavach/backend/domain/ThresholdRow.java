package com.yantrakavach.backend.domain;

import com.yantrakavach.backend.domain.enums.ParamKey;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row per parameter: the warning and critical limits used by the health / alert engine. */
@Entity
@Table(name = "threshold_rows")
public class ThresholdRow {

    @Id
    @Enumerated(EnumType.STRING)
    private ParamKey param;

    private double warn;
    private double crit;

    public ThresholdRow() {
    }

    public ThresholdRow(ParamKey param, double warn, double crit) {
        this.param = param;
        this.warn = warn;
        this.crit = crit;
    }

    public ParamKey getParam() {
        return param;
    }

    public void setParam(ParamKey param) {
        this.param = param;
    }

    public double getWarn() {
        return warn;
    }

    public void setWarn(double warn) {
        this.warn = warn;
    }

    public double getCrit() {
        return crit;
    }

    public void setCrit(double crit) {
        this.crit = crit;
    }
}
