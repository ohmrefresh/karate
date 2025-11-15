package com.karate.mockmgr.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecordingSession {
    private String id;
    private String targetBaseUrl;
    private int proxyPort;
    private String status; // ACTIVE, STOPPED
    private LocalDateTime startedAt;
    private int recordedCount;
}
