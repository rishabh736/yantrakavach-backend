package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.domain.MaintenanceRecord;
import com.yantrakavach.backend.dto.MaintenanceCreateRequest;
import com.yantrakavach.backend.repo.MaintenanceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceController {

    private static final Pattern WO_NUM = Pattern.compile("WO-(\\d+)");

    private final MaintenanceRepository maintenanceRepository;
    private final AtomicInteger seq;

    public MaintenanceController(MaintenanceRepository maintenanceRepository) {
        this.maintenanceRepository = maintenanceRepository;
        int max = 2600;
        for (MaintenanceRecord r : maintenanceRepository.findAll()) {
            var m = WO_NUM.matcher(r.getId());
            if (m.matches()) max = Math.max(max, Integer.parseInt(m.group(1)));
        }
        this.seq = new AtomicInteger(max);
    }

    @GetMapping
    public List<MaintenanceRecord> all() {
        return maintenanceRepository.findAllByOrderByDateDesc();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceRecord create(@Valid @RequestBody MaintenanceCreateRequest body) {
        MaintenanceRecord r = new MaintenanceRecord();
        r.setId("WO-" + seq.incrementAndGet());
        r.setMachineId(body.getMachineId());
        r.setIssue(body.getIssue());
        r.setAction(body.getAction());
        r.setTechnician(body.getTechnician());
        r.setStatus(body.getStatus());
        r.setDowntimeMin(body.getDowntimeMin());
        r.setCost(body.getCost());
        r.setParts(body.getParts() == null || body.getParts().isBlank() ? "—" : body.getParts());
        r.setDate(Instant.now());
        return maintenanceRepository.save(r);
    }
}
