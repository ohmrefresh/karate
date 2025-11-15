package com.karate.mockmgr.model;

import lombok.Data;

@Data
public class ScenarioStats {
    private String name;
    private int count;
    private int success;
    private int failed;
    private double avgDuration;
    private long minDuration;
    private long maxDuration;
}
