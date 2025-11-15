package com.karate.mockmgr.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class PerformanceTest {
    private String id;
    private String name;
    private String featureFile;
    private int users;
    private int rampUpSeconds;
    private int durationSeconds;
    private String status; // PENDING, RUNNING, COMPLETED, FAILED
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private PerformanceResults results;
}
