package com.karate.mockmgr.service;

import com.intuit.karate.core.MockServer;
import com.karate.mockmgr.model.MockServerConfig;
import com.karate.mockmgr.model.MockServerInfo;
import com.karate.mockmgr.model.RequestLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class MockServerManager {

    private final Map<String, EnhancedMockServer> servers = new ConcurrentHashMap<>();
    private final SimpMessagingTemplate messagingTemplate;

    public MockServerManager(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public MockServerInfo startServer(MockServerConfig config) {
        if (servers.containsKey(config.getId())) {
            throw new IllegalStateException("Server with id " + config.getId() + " already exists");
        }

        try {
            File featureFile = new File(config.getFeatureFile());
            if (!featureFile.exists()) {
                throw new IllegalArgumentException("Feature file not found: " + config.getFeatureFile());
            }

            MockServer.Builder builder = MockServer.feature(featureFile)
                    .http(config.getPort());

            if (config.getPathPrefix() != null) {
                builder.pathPrefix(config.getPathPrefix());
            }

            if (config.isWatch()) {
                builder.watch(true);
            }

            MockServer mockServer = builder.build();
            EnhancedMockServer enhancedServer = new EnhancedMockServer(config.getId(), mockServer);

            // Set up request logging with WebSocket notification
            enhancedServer.setLogListener(log -> {
                messagingTemplate.convertAndSend("/topic/requests/" + config.getId(), log);
            });

            servers.put(config.getId(), enhancedServer);

            log.info("Started mock server {} on port {}", config.getId(), config.getPort());

            return getServerInfo(config.getId());

        } catch (Exception e) {
            log.error("Failed to start mock server", e);
            throw new RuntimeException("Failed to start mock server: " + e.getMessage(), e);
        }
    }

    public void stopServer(String id) {
        EnhancedMockServer server = servers.remove(id);
        if (server != null) {
            server.stop();
            log.info("Stopped mock server {}", id);
        }
    }

    public MockServerInfo getServerInfo(String id) {
        EnhancedMockServer server = servers.get(id);
        if (server == null) {
            return null;
        }

        MockServerInfo info = new MockServerInfo();
        info.setId(id);
        info.setPort(server.getPort());
        info.setStatus("RUNNING");
        info.setStartedAt(server.getStartedAt());
        info.setRequestCount(server.getRequestCount());

        return info;
    }

    public List<MockServerInfo> getAllServers() {
        List<MockServerInfo> result = new ArrayList<>();
        for (String id : servers.keySet()) {
            result.add(getServerInfo(id));
        }
        return result;
    }

    public List<RequestLog> getRequestLogs(String serverId, int limit) {
        EnhancedMockServer server = servers.get(serverId);
        if (server == null) {
            return Collections.emptyList();
        }
        return server.getRequestLogs(limit);
    }

    public Map<String, Object> getVariables(String serverId) {
        EnhancedMockServer server = servers.get(serverId);
        if (server == null) {
            return Collections.emptyMap();
        }
        return server.getVariables();
    }

    public void setVariable(String serverId, String key, Object value) {
        EnhancedMockServer server = servers.get(serverId);
        if (server != null) {
            server.setVariable(key, value);
        }
    }
}
