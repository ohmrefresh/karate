package com.karate.mockmgr.service;

import com.intuit.karate.core.MockServer;
import com.intuit.karate.http.Request;
import com.intuit.karate.http.Response;
import com.karate.mockmgr.model.RequestLog;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class EnhancedMockServer {

    @Getter
    private final String id;
    private final MockServer mockServer;

    @Getter
    private final LocalDateTime startedAt;

    private final AtomicLong requestCount = new AtomicLong(0);
    private final Queue<RequestLog> requestLogs = new ConcurrentLinkedQueue<>();
    private static final int MAX_LOGS = 1000;

    private RequestLogListener logListener;

    public interface RequestLogListener {
        void onRequestLogged(RequestLog log);
    }

    public EnhancedMockServer(String id, MockServer mockServer) {
        this.id = id;
        this.mockServer = mockServer;
        this.startedAt = LocalDateTime.now();
    }

    public void setLogListener(RequestLogListener listener) {
        this.logListener = listener;
    }

    public void logRequest(Request request, Response response, long durationMs, String matchedScenario) {
        requestCount.incrementAndGet();

        RequestLog log = new RequestLog();
        log.setId(UUID.randomUUID().toString());
        log.setServerId(id);
        log.setTimestamp(LocalDateTime.now());
        log.setMethod(request.getMethod());
        log.setPath(request.getPath());
        log.setHeaders(request.getHeaders());
        log.setBody(request.getBodyAsString());
        log.setResponseStatus(response.getStatus());
        log.setResponseBody(response.getBodyAsString());
        log.setDurationMs(durationMs);
        log.setMatchedScenario(matchedScenario);

        // Keep only last MAX_LOGS entries
        if (requestLogs.size() >= MAX_LOGS) {
            requestLogs.poll();
        }
        requestLogs.add(log);

        // Notify listener
        if (logListener != null) {
            logListener.onRequestLogged(log);
        }
    }

    public long getRequestCount() {
        return requestCount.get();
    }

    public List<RequestLog> getRequestLogs(int limit) {
        List<RequestLog> logs = new ArrayList<>(requestLogs);
        Collections.reverse(logs); // Most recent first
        return logs.subList(0, Math.min(limit, logs.size()));
    }

    public List<RequestLog> getAllRequestLogs() {
        List<RequestLog> logs = new ArrayList<>(requestLogs);
        Collections.reverse(logs);
        return logs;
    }

    public MockServer getMockServer() {
        return mockServer;
    }

    public int getPort() {
        return mockServer.getPort();
    }

    public void stop() {
        mockServer.stop();
    }

    public Map<String, Object> getVariables() {
        try {
            // Access Karate's global variables
            return new HashMap<>(mockServer.getEngine().runtime.magicVariables);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public void setVariable(String key, Object value) {
        try {
            mockServer.getEngine().runtime.magicVariables.put(key, value);
        } catch (Exception e) {
            // Handle error
        }
    }
}
