package com.karate.mockmgr.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.karate.mockmgr.model.RecordedRequest;
import com.karate.mockmgr.model.RecordingSession;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecordingProxyService {

    private final Map<String, RecordingSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, List<RecordedRequest>> recordings = new ConcurrentHashMap<>();
    private final CloseableHttpClient httpClient = HttpClients.createDefault();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public RecordingSession startRecording(String sessionId, String targetBaseUrl, int proxyPort) {
        RecordingSession session = new RecordingSession();
        session.setId(sessionId);
        session.setTargetBaseUrl(targetBaseUrl);
        session.setProxyPort(proxyPort);
        session.setStatus("ACTIVE");
        session.setStartedAt(LocalDateTime.now());
        session.setRecordedCount(0);

        sessions.put(sessionId, session);
        recordings.put(sessionId, new ArrayList<>());

        log.info("Started recording session {} for target {}", sessionId, targetBaseUrl);
        return session;
    }

    public void stopRecording(String sessionId) {
        RecordingSession session = sessions.get(sessionId);
        if (session != null) {
            session.setStatus("STOPPED");
            log.info("Stopped recording session {}", sessionId);
        }
    }

    public RecordedRequest recordRequest(String sessionId, String method, String path,
                                        Map<String, String> headers, String body) {
        RecordingSession session = sessions.get(sessionId);
        if (session == null || !"ACTIVE".equals(session.getStatus())) {
            return null;
        }

        long startTime = System.currentTimeMillis();
        RecordedRequest recorded = new RecordedRequest();
        recorded.setId(UUID.randomUUID().toString());
        recorded.setSessionId(sessionId);
        recorded.setTimestamp(LocalDateTime.now());
        recorded.setMethod(method);
        recorded.setPath(path);
        recorded.setHeaders(headers);
        recorded.setBody(body);

        try {
            // Forward request to target
            String targetUrl = session.getTargetBaseUrl() + path;
            HttpUriRequestBase request = createHttpRequest(method, targetUrl, headers, body);

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                recorded.setResponseStatus(response.getCode());

                // Capture response headers
                Map<String, String> responseHeaders = new HashMap<>();
                Arrays.stream(response.getHeaders()).forEach(header ->
                    responseHeaders.put(header.getName(), header.getValue())
                );
                recorded.setResponseHeaders(responseHeaders);

                // Capture response body
                if (response.getEntity() != null) {
                    String responseBody = EntityUtils.toString(response.getEntity());
                    recorded.setResponseBody(responseBody);
                }

                recorded.setDurationMs(System.currentTimeMillis() - startTime);
            }

            // Store recording
            recordings.get(sessionId).add(recorded);
            session.setRecordedCount(session.getRecordedCount() + 1);

            log.debug("Recorded request: {} {}", method, path);
            return recorded;

        } catch (Exception e) {
            log.error("Failed to record request", e);
            recorded.setResponseStatus(500);
            recorded.setResponseBody("{\"error\": \"" + e.getMessage() + "\"}");
            recorded.setDurationMs(System.currentTimeMillis() - startTime);
            return recorded;
        }
    }

    private HttpUriRequestBase createHttpRequest(String method, String url,
                                                  Map<String, String> headers, String body) {
        HttpUriRequestBase request;

        switch (method.toUpperCase()) {
            case "POST":
                request = new HttpPost(url);
                if (body != null) ((HttpPost) request).setEntity(new StringEntity(body));
                break;
            case "PUT":
                request = new HttpPut(url);
                if (body != null) ((HttpPut) request).setEntity(new StringEntity(body));
                break;
            case "PATCH":
                request = new HttpPatch(url);
                if (body != null) ((HttpPatch) request).setEntity(new StringEntity(body));
                break;
            case "DELETE":
                request = new HttpDelete(url);
                break;
            default:
                request = new HttpGet(url);
        }

        // Add headers (excluding some that should not be forwarded)
        if (headers != null) {
            headers.forEach((name, value) -> {
                if (!name.equalsIgnoreCase("Host") &&
                    !name.equalsIgnoreCase("Content-Length")) {
                    request.addHeader(name, value);
                }
            });
        }

        return request;
    }

    public List<RecordingSession> getAllSessions() {
        return new ArrayList<>(sessions.values());
    }

    public RecordingSession getSession(String sessionId) {
        return sessions.get(sessionId);
    }

    public List<RecordedRequest> getRecordings(String sessionId) {
        return recordings.getOrDefault(sessionId, Collections.emptyList());
    }

    public String generateFeatureFromRecordings(String sessionId, String featureName) {
        List<RecordedRequest> requests = getRecordings(sessionId);
        if (requests.isEmpty()) {
            throw new RuntimeException("No recordings found for session: " + sessionId);
        }

        StringBuilder feature = new StringBuilder();
        feature.append("Feature: ").append(featureName != null ? featureName : "Recorded from " + sessionId).append("\n\n");

        feature.append("Background:\n");
        feature.append("  # Generated from recorded API traffic\n");
        feature.append("  * def recordedData = {}\n\n");

        // Group requests by path and method
        Map<String, List<RecordedRequest>> grouped = requests.stream()
            .collect(Collectors.groupingBy(r -> r.getMethod() + " " + r.getPath()));

        grouped.forEach((key, requestList) -> {
            RecordedRequest example = requestList.get(0);
            String path = example.getPath();
            String method = example.getMethod().toLowerCase();

            feature.append("Scenario: ").append(key).append("\n");
            feature.append("  * def pathMatch = pathMatches('").append(path).append("')\n");
            feature.append("  * def methodMatch = methodIs('").append(method).append("')\n");
            feature.append("  * if (!pathMatch || !methodMatch) karate.abort()\n\n");

            // Add response
            feature.append("  * def responseStatus = ").append(example.getResponseStatus()).append("\n");

            String responseBody = example.getResponseBody();
            if (responseBody != null && !responseBody.isEmpty()) {
                try {
                    // Try to parse as JSON and format
                    Object parsed = gson.fromJson(responseBody, Object.class);
                    String formatted = gson.toJson(parsed);
                    feature.append("  * def response = \n");
                    feature.append("    \"\"\"\n");
                    feature.append("    ").append(formatted).append("\n");
                    feature.append("    \"\"\"\n");
                } catch (Exception e) {
                    // If not JSON, use as string
                    feature.append("  * def response = '").append(responseBody.replace("'", "\\'")).append("'\n");
                }
            } else {
                feature.append("  * def response = ''\n");
            }

            // Add comment about number of recorded instances
            if (requestList.size() > 1) {
                feature.append("  # ").append(requestList.size()).append(" instances recorded\n");
            }

            feature.append("\n");
        });

        // Add fallback scenario
        feature.append("Scenario:\n");
        feature.append("  * def responseStatus = 404\n");
        feature.append("  * def response = { error: 'Not found', path: requestPath }\n");

        return feature.toString();
    }

    public void clearRecordings(String sessionId) {
        recordings.remove(sessionId);
        sessions.remove(sessionId);
    }
}
