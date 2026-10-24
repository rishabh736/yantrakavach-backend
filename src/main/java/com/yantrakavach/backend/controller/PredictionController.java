package com.yantrakavach.backend.controller;

import com.yantrakavach.backend.domain.Machine;
import com.yantrakavach.backend.dto.PredictionDto;
import com.yantrakavach.backend.repo.MachineRepository;
import com.yantrakavach.backend.service.PredictionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    private final MachineRepository machineRepository;
    private final PredictionService predictionService;

    public PredictionController(MachineRepository machineRepository, PredictionService predictionService) {
        this.machineRepository = machineRepository;
        this.predictionService = predictionService;
    }

    @GetMapping
    public List<PredictionDto> all() {
        List<Machine> machines = machineRepository.findAll();
        return machines.stream().map(predictionService::predict).toList();
    }
}
