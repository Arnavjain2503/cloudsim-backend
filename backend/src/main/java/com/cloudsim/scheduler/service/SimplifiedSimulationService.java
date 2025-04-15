package com.cloudsim.scheduler.service;

import com.cloudsim.scheduler.model.CloudletResult;
import com.cloudsim.scheduler.model.SimulationRequest;
import com.cloudsim.scheduler.model.SimulationResult;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A simplified simulation service that implements various CPU scheduling algorithms
 * without direct CloudSim Plus integration.
 */
@Service
public class SimplifiedSimulationService {

    // Constants for simulation
    private static final double PROCESSING_POWER_FACTOR = 0.01;
    private static final double COST_PER_SECOND = 0.01;

    /**
     * Run a simulation with the specified parameters and scheduling algorithm.
     */
    public SimulationResult runSimulation(SimulationRequest request) {
        // Create VMs and Cloudlets based on request parameters
        List<VirtualMachine> vms = createVms(request);
        List<Task> tasks = createTasks(request);
        
        // Apply the selected scheduling algorithm
        List<TaskResult> results;
        switch (request.getSchedulingAlgorithm()) {
            case "MIN_MIN":
                results = applyMinMinScheduling(vms, tasks);
                break;
            case "MAX_MIN":
                results = applyMaxMinScheduling(vms, tasks);
                break;
            case "SUFFERAGE":
                results = applySufferageScheduling(vms, tasks);
                break;
            case "OPPORTUNISTIC_LOAD_BALANCING":
                results = applyOpportunisticLoadBalancing(vms, tasks);
                break;
            case "MINIMUM_COMPLETION_TIME":
                results = applyMinimumCompletionTime(vms, tasks);
                break;
            default:
                // Default to Min-Min if no valid algorithm is selected
                results = applyMinMinScheduling(vms, tasks);
                break;
        }
        
        // Convert task results to cloudlet results for API consistency
        List<CloudletResult> cloudletResults = results.stream()
                .map(this::mapToCloudletResult)
                .collect(Collectors.toList());
        
        // Calculate total execution time and cost
        double totalExecutionTime = cloudletResults.stream()
                .mapToDouble(CloudletResult::getExecutionTime)
                .sum();
        
        double totalCost = cloudletResults.stream()
                .mapToDouble(CloudletResult::getCost)
                .sum();
        
        return new SimulationResult(
                cloudletResults,
                totalExecutionTime,
                totalCost,
                request.getSchedulingAlgorithm()
        );
    }
    
    /**
     * Create virtual machines based on the simulation request.
     */
    private List<VirtualMachine> createVms(SimulationRequest request) {
        List<VirtualMachine> vms = new ArrayList<>();
        
        for (int i = 0; i < request.getNumberOfVms(); i++) {
            VirtualMachine vm = new VirtualMachine(
                    i,
                    request.getVmMips(),
                    request.getVmRam(),
                    request.getVmBw(),
                    request.getVmSize()
            );
            vms.add(vm);
        }
        
        return vms;
    }
    
    /**
     * Create tasks (cloudlets) based on the simulation request.
     */
    private List<Task> createTasks(SimulationRequest request) {
        List<Task> tasks = new ArrayList<>();
        
        for (int i = 0; i < request.getNumberOfCloudlets(); i++) {
            Task task = new Task(
                    i,
                    request.getCloudletLength(),
                    request.getCloudletPes(),
                    request.getCloudletFileSize(),
                    request.getCloudletOutputSize()
            );
            tasks.add(task);
        }
        
        return tasks;
    }
    
    /**
     * Map a task result to a cloudlet result for API consistency.
     */
    private CloudletResult mapToCloudletResult(TaskResult taskResult) {
        return new CloudletResult(
                taskResult.getTaskId(),
                taskResult.getVmId(),
                taskResult.getStartTime(),
                taskResult.getFinishTime(),
                taskResult.getStatus(),
                taskResult.getExecutionTime(),
                taskResult.getCost()
        );
    }
    
    /**
     * Min-Min Algorithm Implementation:
     * Assigns the task with the minimum completion time to the VM that can complete it the earliest.
     */
    private List<TaskResult> applyMinMinScheduling(List<VirtualMachine> vms, List<Task> tasks) {
        // Sort tasks by length (shortest first)
        tasks.sort(Comparator.comparingLong(Task::getLength));
        
        return scheduleTasksToVms(vms, tasks);
    }
    
    /**
     * Max-Min Algorithm Implementation:
     * Assigns the task with the maximum completion time to the VM that can complete it the earliest.
     */
    private List<TaskResult> applyMaxMinScheduling(List<VirtualMachine> vms, List<Task> tasks) {
        // Sort tasks by length (longest first)
        tasks.sort(Comparator.comparingLong(Task::getLength).reversed());
        
        return scheduleTasksToVms(vms, tasks);
    }
    
    /**
     * Sufferage Algorithm Implementation:
     * Assigns tasks based on the difference between the best and second-best completion times.
     */
    private List<TaskResult> applySufferageScheduling(List<VirtualMachine> vms, List<Task> tasks) {
        List<TaskResult> results = new ArrayList<>();
        Map<Integer, Double> vmCompletionTimes = new HashMap<>();
        
        // Initialize VM completion times to 0
        for (VirtualMachine vm : vms) {
            vmCompletionTimes.put(vm.getId(), 0.0);
        }
        
        List<Task> remainingTasks = new ArrayList<>(tasks);
        
        while (!remainingTasks.isEmpty()) {
            Task selectedTask = null;
            VirtualMachine selectedVm = null;
            double maxSufferage = -1;
            
            for (Task task : remainingTasks) {
                // Find fastest and second fastest VM for this task
                double fastestTime = Double.MAX_VALUE;
                double secondFastestTime = Double.MAX_VALUE;
                VirtualMachine fastestVm = null;
                
                for (VirtualMachine vm : vms) {
                    double completionTime = calculateCompletionTime(task, vm, vmCompletionTimes.get(vm.getId()));
                    
                    if (completionTime < fastestTime) {
                        secondFastestTime = fastestTime;
                        fastestTime = completionTime;
                        fastestVm = vm;
                    } else if (completionTime < secondFastestTime) {
                        secondFastestTime = completionTime;
                    }
                }
                
                // Calculate sufferage value
                double sufferage = secondFastestTime - fastestTime;
                
                // Select task with maximum sufferage
                if (sufferage > maxSufferage) {
                    maxSufferage = sufferage;
                    selectedTask = task;
                    selectedVm = fastestVm;
                }
            }
            
            if (selectedTask != null && selectedVm != null) {
                // Calculate start and finish times
                double startTime = vmCompletionTimes.get(selectedVm.getId());
                double executionTime = calculateExecutionTime(selectedTask, selectedVm);
                double finishTime = startTime + executionTime;
                
                // Create task result
                TaskResult result = new TaskResult(
                        selectedTask.getId(),
                        selectedVm.getId(),
                        startTime,
                        finishTime,
                        "SUCCESS",
                        executionTime,
                        executionTime * COST_PER_SECOND
                );
                
                results.add(result);
                remainingTasks.remove(selectedTask);
                
                // Update VM completion time
                vmCompletionTimes.put(selectedVm.getId(), finishTime);
            } else {
                // If no task was selected, break to avoid infinite loop
                break;
            }
        }
        
        return results;
    }
    
    /**
     * Opportunistic Load Balancing Algorithm Implementation:
     * Assigns tasks to VMs with the least load (number of assigned tasks).
     */
    private List<TaskResult> applyOpportunisticLoadBalancing(List<VirtualMachine> vms, List<Task> tasks) {
        List<TaskResult> results = new ArrayList<>();
        Map<Integer, Double> vmCompletionTimes = new HashMap<>();
        Map<Integer, Integer> vmLoads = new HashMap<>();
        
        // Initialize VM completion times and loads to 0
        for (VirtualMachine vm : vms) {
            vmCompletionTimes.put(vm.getId(), 0.0);
            vmLoads.put(vm.getId(), 0);
        }
        
        for (Task task : tasks) {
            // Find VM with minimum load
            VirtualMachine selectedVm = vms.stream()
                    .min(Comparator.comparingInt(vm -> vmLoads.get(vm.getId())))
                    .orElse(vms.get(0));
            
            // Calculate start and finish times
            double startTime = vmCompletionTimes.get(selectedVm.getId());
            double executionTime = calculateExecutionTime(task, selectedVm);
            double finishTime = startTime + executionTime;
            
            // Create task result
            TaskResult result = new TaskResult(
                    task.getId(),
                    selectedVm.getId(),
                    startTime,
                    finishTime,
                    "SUCCESS",
                    executionTime,
                    executionTime * COST_PER_SECOND
            );
            
            results.add(result);
            
            // Update VM completion time and load
            vmCompletionTimes.put(selectedVm.getId(), finishTime);
            vmLoads.put(selectedVm.getId(), vmLoads.get(selectedVm.getId()) + 1);
        }
        
        return results;
    }
    
    /**
     * Minimum Completion Time Algorithm Implementation:
     * Assigns each task to the VM that can complete it the earliest.
     */
    private List<TaskResult> applyMinimumCompletionTime(List<VirtualMachine> vms, List<Task> tasks) {
        List<TaskResult> results = new ArrayList<>();
        Map<Integer, Double> vmCompletionTimes = new HashMap<>();
        
        // Initialize VM completion times to 0
        for (VirtualMachine vm : vms) {
            vmCompletionTimes.put(vm.getId(), 0.0);
        }
        
        for (Task task : tasks) {
            VirtualMachine selectedVm = null;
            double earliestCompletionTime = Double.MAX_VALUE;
            
            for (VirtualMachine vm : vms) {
                double completionTime = calculateCompletionTime(task, vm, vmCompletionTimes.get(vm.getId()));
                
                if (completionTime < earliestCompletionTime) {
                    earliestCompletionTime = completionTime;
                    selectedVm = vm;
                }
            }
            
            if (selectedVm != null) {
                // Calculate start and finish times
                double startTime = vmCompletionTimes.get(selectedVm.getId());
                double executionTime = calculateExecutionTime(task, selectedVm);
                double finishTime = startTime + executionTime;
                
                // Create task result
                TaskResult result = new TaskResult(
                        task.getId(),
                        selectedVm.getId(),
                        startTime,
                        finishTime,
                        "SUCCESS",
                        executionTime,
                        executionTime * COST_PER_SECOND
                );
                
                results.add(result);
                
                // Update VM completion time
                vmCompletionTimes.put(selectedVm.getId(), finishTime);
            }
        }
        
        return results;
    }
    
    /**
     * Helper method to schedule tasks to VMs based on the current VM completion times.
     */
    private List<TaskResult> scheduleTasksToVms(List<VirtualMachine> vms, List<Task> tasks) {
        List<TaskResult> results = new ArrayList<>();
        Map<Integer, Double> vmCompletionTimes = new HashMap<>();
        
        // Initialize VM completion times to 0
        for (VirtualMachine vm : vms) {
            vmCompletionTimes.put(vm.getId(), 0.0);
        }
        
        for (Task task : tasks) {
            VirtualMachine selectedVm = null;
            double earliestCompletionTime = Double.MAX_VALUE;
            
            for (VirtualMachine vm : vms) {
                double completionTime = calculateCompletionTime(task, vm, vmCompletionTimes.get(vm.getId()));
                
                if (completionTime < earliestCompletionTime) {
                    earliestCompletionTime = completionTime;
                    selectedVm = vm;
                }
            }
            
            if (selectedVm != null) {
                // Calculate start and finish times
                double startTime = vmCompletionTimes.get(selectedVm.getId());
                double executionTime = calculateExecutionTime(task, selectedVm);
                double finishTime = startTime + executionTime;
                
                // Create task result
                TaskResult result = new TaskResult(
                        task.getId(),
                        selectedVm.getId(),
                        startTime,
                        finishTime,
                        "SUCCESS",
                        executionTime,
                        executionTime * COST_PER_SECOND
                );
                
                results.add(result);
                
                // Update VM completion time
                vmCompletionTimes.put(selectedVm.getId(), finishTime);
            }
        }
        
        return results;
    }
    
    /**
     * Calculate the execution time of a task on a VM.
     */
    private double calculateExecutionTime(Task task, VirtualMachine vm) {
        return task.getLength() * PROCESSING_POWER_FACTOR / vm.getMips();
    }
    
    /**
     * Calculate the completion time of a task on a VM, considering the VM's current completion time.
     */
    private double calculateCompletionTime(Task task, VirtualMachine vm, double currentCompletionTime) {
        return currentCompletionTime + calculateExecutionTime(task, vm);
    }
    
    /**
     * Inner class representing a virtual machine.
     */
    private static class VirtualMachine {
        private final int id;
        private final int mips;
        private final int ram;
        private final int bw;
        private final int size;
        
        public VirtualMachine(int id, int mips, int ram, int bw, int size) {
            this.id = id;
            this.mips = mips;
            this.ram = ram;
            this.bw = bw;
            this.size = size;
        }
        
        public int getId() {
            return id;
        }
        
        public int getMips() {
            return mips;
        }
        
        public int getRam() {
            return ram;
        }
        
        public int getBw() {
            return bw;
        }
        
        public int getSize() {
            return size;
        }
    }
    
    /**
     * Inner class representing a task (cloudlet).
     */
    private static class Task {
        private final int id;
        private final long length;
        private final int pes;
        private final long fileSize;
        private final long outputSize;
        
        public Task(int id, long length, int pes, long fileSize, long outputSize) {
            this.id = id;
            this.length = length;
            this.pes = pes;
            this.fileSize = fileSize;
            this.outputSize = outputSize;
        }
        
        public int getId() {
            return id;
        }
        
        public long getLength() {
            return length;
        }
        
        public int getPes() {
            return pes;
        }
        
        public long getFileSize() {
            return fileSize;
        }
        
        public long getOutputSize() {
            return outputSize;
        }
    }
    
    /**
     * Inner class representing the result of a task execution.
     */
    private static class TaskResult {
        private final int taskId;
        private final int vmId;
        private final double startTime;
        private final double finishTime;
        private final String status;
        private final double executionTime;
        private final double cost;
        
        public TaskResult(int taskId, int vmId, double startTime, double finishTime, String status, double executionTime, double cost) {
            this.taskId = taskId;
            this.vmId = vmId;
            this.startTime = startTime;
            this.finishTime = finishTime;
            this.status = status;
            this.executionTime = executionTime;
            this.cost = cost;
        }
        
        public int getTaskId() {
            return taskId;
        }
        
        public int getVmId() {
            return vmId;
        }
        
        public double getStartTime() {
            return startTime;
        }
        
        public double getFinishTime() {
            return finishTime;
        }
        
        public String getStatus() {
            return status;
        }
        
        public double getExecutionTime() {
            return executionTime;
        }
        
        public double getCost() {
            return cost;
        }
    }
}
