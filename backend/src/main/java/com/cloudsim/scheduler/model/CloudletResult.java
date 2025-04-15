package com.cloudsim.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CloudletResult {
    private int cloudletId;
    private int vmId;
    private double startTime;
    private double finishTime;
    private String status;
    private double executionTime;
    private double cost;
}
