package com.karate.mockmgr.controller;

import com.karate.mockmgr.model.RecordedRequest;
import com.karate.mockmgr.model.RecordingSession;
import com.karate.mockmgr.service.RecordingProxyService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recording")
@RequiredArgsConstructor
public class RecordingController {

    private final RecordingProxyService recordingService;
    private static final String MOCKS_DIR = "mocks";

    @PostMapping("/sessions")
    public ResponseEntity<RecordingSession> startSession(@RequestBody Map<String, Object> payload) {
        String sessionId = (String) payload.get("sessionId");
        String targetBaseUrl = (String) payload.get("targetBaseUrl");
        int proxyPort = (Integer) payload.getOrDefault("proxyPort", 9999);

        RecordingSession session = recordingService.startRecording(sessionId, targetBaseUrl, proxyPort);
        return ResponseEntity.ok(session);
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<Void> stopSession(@PathVariable String sessionId) {
        recordingService.stopRecording(sessionId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<RecordingSession>> getAllSessions() {
        return ResponseEntity.ok(recordingService.getAllSessions());
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<RecordingSession> getSession(@PathVariable String sessionId) {
        RecordingSession session = recordingService.getSession(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}/recordings")
    public ResponseEntity<List<RecordedRequest>> getRecordings(@PathVariable String sessionId) {
        return ResponseEntity.ok(recordingService.getRecordings(sessionId));
    }

    @PostMapping("/sessions/{sessionId}/generate")
    public ResponseEntity<Map<String, String>> generateFeature(
            @PathVariable String sessionId,
            @RequestBody Map<String, String> payload) {

        try {
            String featureName = payload.get("featureName");
            String karateFeature = recordingService.generateFeatureFromRecordings(sessionId, featureName);

            // Save to file
            String filename = (featureName != null ? featureName : "recorded") + ".feature";
            Path filePath = Paths.get(MOCKS_DIR, filename);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, karateFeature);

            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("path", filePath.toString());
            response.put("content", karateFeature);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @DeleteMapping("/sessions/{sessionId}/recordings")
    public ResponseEntity<Void> clearRecordings(@PathVariable String sessionId) {
        recordingService.clearRecordings(sessionId);
        return ResponseEntity.ok().build();
    }

    // Proxy endpoint to record requests
    @RequestMapping(value = "/proxy/{sessionId}/**", method = {
        RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT,
        RequestMethod.DELETE, RequestMethod.PATCH
    })
    public ResponseEntity<String> proxyRequest(
            @PathVariable String sessionId,
            HttpServletRequest request) throws IOException {

        String path = request.getRequestURI().replace("/api/recording/proxy/" + sessionId, "");
        String method = request.getMethod();

        // Extract headers
        Map<String, String> headers = new HashMap<>();
        Collections.list(request.getHeaderNames()).forEach(name ->
            headers.put(name, request.getHeader(name))
        );

        // Extract body
        String body = null;
        if (request.getContentLength() > 0) {
            try (BufferedReader reader = request.getReader()) {
                body = reader.lines().collect(Collectors.joining("\n"));
            }
        }

        // Record and forward request
        RecordedRequest recorded = recordingService.recordRequest(sessionId, method, path, headers, body);

        if (recorded == null) {
            return ResponseEntity.badRequest().body("{\"error\": \"Session not active\"}");
        }

        return ResponseEntity.status(recorded.getResponseStatus())
            .body(recorded.getResponseBody());
    }
}
