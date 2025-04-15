package com.cloudsim.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResult {
    private List<CloudletResult> cloudletResults;
    private double totalExecutionTime;
    private double totalCost;
    private String schedulingAlgorithm;
}
