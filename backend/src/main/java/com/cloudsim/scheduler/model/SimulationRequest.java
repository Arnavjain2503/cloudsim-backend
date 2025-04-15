package com.cloudsim.scheduler.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {
    private int numberOfVms;
    private int numberOfCloudlets;
    private int vmMips;
    private int vmRam;
    private int vmBw;
    private int vmSize;
    private int cloudletLength;
    private int cloudletPes;
    private int cloudletFileSize;
    private int cloudletOutputSize;
    private String schedulingAlgorithm;
}
