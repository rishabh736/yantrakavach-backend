package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.repo.MachineRepository;
import com.yantrakavach.backend.service.TelemetryService;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/machines")
public class MachineController {

    private final MachineRepository machineRepository;
    private final TelemetryService telemetryService;

    public MachineController(MachineRepository machineRepository, TelemetryService telemetryService) {
        this.machineRepository = machineRepository;
        this.telemetryService = telemetryService;
    }

    @GetMapping
    public List<Machine> all() {
        return machineRepository.findAll();
    }

    @GetMapping("/{id}")
    public Machine one(@PathVariable("id") String id) {
        return machineRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("No machine with id " + id));
    }

    /** Demo/test helper: drifts a machine's target into a fault state over the next few telemetry ticks. */
    @PostMapping("/{id}/fault")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void injectFault(@PathVariable("id") String id) {
        machineRepository.findById(id).orElseThrow(() -> ApiException.notFound("No machine with id " + id));
        telemetryService.injectFault(id);
    }

    /** Demo/test helper: restores a machine's target to its originally seeded value. */
    @PostMapping("/{id}/fault/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void resetFault(@PathVariable("id") String id) {
        Machine m = machineRepository.findById(id).orElseThrow(() -> ApiException.notFound("No machine with id " + id));
        telemetryService.resetTarget(id, m.getReadings());
    }
}
