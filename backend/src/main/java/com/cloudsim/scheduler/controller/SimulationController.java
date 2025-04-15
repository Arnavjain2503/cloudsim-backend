package com.cloudsim.scheduler.controller;

import com.cloudsim.scheduler.model.SimulationRequest;
import com.cloudsim.scheduler.model.SimulationResult;
import com.cloudsim.scheduler.service.SimplifiedSimulationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/simulation")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class SimulationController {
    private final SimplifiedSimulationService simulationService;

    @Autowired
    public SimulationController(SimplifiedSimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/run")
    public ResponseEntity<SimulationResult> runSimulation(@RequestBody SimulationRequest request) {
        SimulationResult result = simulationService.runSimulation(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/algorithms")
    public ResponseEntity<String[]> getAvailableAlgorithms() {
        String[] algorithms = {
            "MIN_MIN", 
            "MAX_MIN", 
            "SUFFERAGE", 
            "OPPORTUNISTIC_LOAD_BALANCING", 
            "MINIMUM_COMPLETION_TIME"
        };
        return ResponseEntity.ok(algorithms);
    }
}
