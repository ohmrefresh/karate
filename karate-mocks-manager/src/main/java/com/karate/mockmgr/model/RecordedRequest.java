package com.karate.mockmgr.model;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Data
public class RecordedRequest {
    private String id;
    private String sessionId;
    private LocalDateTime timestamp;
    private String method;
    private String path;
    private Map<String, String> headers;
    private String body;
    private int responseStatus;
    private Map<String, String> responseHeaders;
    private String responseBody;
    private long durationMs;
}
