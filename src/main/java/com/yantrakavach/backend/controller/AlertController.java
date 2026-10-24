package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.domain.Alert;
import com.yantrakavach.backend.domain.enums.AlertStatus;
import com.yantrakavach.backend.dto.AckResponse;
import com.yantrakavach.backend.dto.AssignRequest;
import com.yantrakavach.backend.exception.ApiException;
import com.yantrakavach.backend.repo.AlertRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepository alertRepository;

    public AlertController(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    @GetMapping
    public List<Alert> all() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    @PostMapping("/{id}/acknowledge")
    public AckResponse acknowledge(@PathVariable("id") String id) {
        Alert a = find(id);
        a.setStatus(AlertStatus.ACKNOWLEDGED);
        alertRepository.save(a);
        return new AckResponse(true);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    public AckResponse resolve(@PathVariable("id") String id) {
        Alert a = find(id);
        a.setStatus(AlertStatus.RESOLVED);
        alertRepository.save(a);
        return new AckResponse(true);
    }

    @PostMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'MAINTENANCE_MANAGER')")
    public Alert assign(@PathVariable("id") String id, @Valid @RequestBody AssignRequest body) {
        Alert a = find(id);
        a.setTechnician(body.getTechnician());
        if (a.getStatus() == AlertStatus.OPEN) a.setStatus(AlertStatus.ACKNOWLEDGED);
        return alertRepository.save(a);
    }

    private Alert find(String id) {
        return alertRepository.findById(id).orElseThrow(() -> ApiException.notFound("No alert with id " + id));
    }
}
