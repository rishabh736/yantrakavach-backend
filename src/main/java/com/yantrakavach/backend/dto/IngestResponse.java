package com.yantrakavach.backend.dto;

import java.time.Instant;

public record IngestResponse(boolean ok, String machineId, Instant receivedAt) {
}
