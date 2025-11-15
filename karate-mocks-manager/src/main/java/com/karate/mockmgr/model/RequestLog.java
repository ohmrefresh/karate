package com.karate.mockmgr.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RequestLog {
    private String id;
    private String serverId;
    private LocalDateTime timestamp;
    private String method;
    private String path;
    private Map<String, Object> headers;
    private String body;
    private int responseStatus;
    private String responseBody;
    private long durationMs;
    private String matchedScenario;
}
