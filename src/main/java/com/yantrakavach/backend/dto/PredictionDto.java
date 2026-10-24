package com.yantrakavach.backend.dto;

import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.domain.enums.ParamKey;
import com.yantrakavach.backend.domain.enums.Status;

import java.util.List;

/** Matches the frontend's Prediction interface in src/types.ts. */
public record PredictionDto(
        Machine machine,
        Status status,
        int health,
        ParamKey worst,
        List<ParamKey> abnormal,
        String risk,
        String window,
        String pattern,
        String what,
        String where,
        String why,
        String impact,
        String action,
        int confidence,
        int stage
) {
}
