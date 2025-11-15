package com.karate.mockmgr.model;

import lombok.Data;
import java.util.Map;

@Data
public class PerformanceResults {
    private int totalRequests;
    private int successfulRequests;
    private int failedRequests;
    private double requestsPerSecond;
    private long minResponseTime;
    private long maxResponseTime;
    private double avgResponseTime;
    private double p50ResponseTime;
    private double p95ResponseTime;
    private double p99ResponseTime;
    private Map<Integer, Integer> statusCodeDistribution;
    private Map<String, ScenarioStats> scenarioStats;
}
