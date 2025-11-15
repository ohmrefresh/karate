package com.karate.mockmgr.controller;

import com.karate.mockmgr.model.MockServerConfig;
import com.karate.mockmgr.model.MockServerInfo;
import com.karate.mockmgr.model.RequestLog;
import com.karate.mockmgr.service.MockServerManager;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/servers")
@RequiredArgsConstructor
public class MockServerController {

    private final MockServerManager serverManager;

    @PostMapping
    public ResponseEntity<MockServerInfo> startServer(@RequestBody MockServerConfig config) {
        MockServerInfo info = serverManager.startServer(config);
        return ResponseEntity.ok(info);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> stopServer(@PathVariable String id) {
        serverManager.stopServer(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<MockServerInfo>> getAllServers() {
        return ResponseEntity.ok(serverManager.getAllServers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MockServerInfo> getServer(@PathVariable String id) {
        MockServerInfo info = serverManager.getServerInfo(id);
        if (info == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(info);
    }

    @GetMapping("/{id}/requests")
    public ResponseEntity<List<RequestLog>> getRequestLogs(
            @PathVariable String id,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(serverManager.getRequestLogs(id, limit));
    }

    @GetMapping("/{id}/variables")
    public ResponseEntity<Map<String, Object>> getVariables(@PathVariable String id) {
        return ResponseEntity.ok(serverManager.getVariables(id));
    }

    @PutMapping("/{id}/variables/{key}")
    public ResponseEntity<Void> setVariable(
            @PathVariable String id,
            @PathVariable String key,
            @RequestBody Map<String, Object> payload) {
        serverManager.setVariable(id, key, payload.get("value"));
        return ResponseEntity.ok().build();
    }
}
