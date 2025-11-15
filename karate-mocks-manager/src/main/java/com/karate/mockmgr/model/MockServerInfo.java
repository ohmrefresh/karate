package com.karate.mockmgr.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class MockServerInfo {
    private String id;
    private int port;
    private String featureFile;
    private String status; // RUNNING, STOPPED
    private LocalDateTime startedAt;
    private long requestCount;
    private String pathPrefix;
    private boolean ssl;
}
