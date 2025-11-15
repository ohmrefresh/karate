package com.karate.mockmgr.service;

import com.karate.mockmgr.model.PerformanceResults;
import com.karate.mockmgr.model.PerformanceTest;
import com.karate.mockmgr.model.ScenarioStats;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class PerformanceTestService {

    private final Map<String, PerformanceTest> tests = new ConcurrentHashMap<>();
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final SimpMessagingTemplate messagingTemplate;

    public PerformanceTestService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public PerformanceTest createTest(String name, String featureFile, int users,
                                     int rampUpSeconds, int durationSeconds) {
        PerformanceTest test = new PerformanceTest();
        test.setId(UUID.randomUUID().toString());
        test.setName(name);
        test.setFeatureFile(featureFile);
        test.setUsers(users);
        test.setRampUpSeconds(rampUpSeconds);
        test.setDurationSeconds(durationSeconds);
        test.setStatus("PENDING");

        tests.put(test.getId(), test);
        log.info("Created performance test: {}", test.getId());

        return test;
    }

    public void startTest(String testId) {
        PerformanceTest test = tests.get(testId);
        if (test == null) {
            throw new RuntimeException("Test not found: " + testId);
        }

        if (!"PENDING".equals(test.getStatus())) {
            throw new RuntimeException("Test already started: " + testId);
        }

        test.setStatus("RUNNING");
        test.setStartedAt(LocalDateTime.now());

        // Run test asynchronously
        executorService.submit(() -> executeTest(test));
    }

    private void executeTest(PerformanceTest test) {
        try {
            log.info("Starting performance test: {}", test.getId());

            // Simulate performance test execution
            // In a real implementation, this would integrate with Karate Gatling
            PerformanceResults results = simulateTestExecution(test);

            test.setResults(results);
            test.setStatus("COMPLETED");
            test.setCompletedAt(LocalDateTime.now());

            // Notify via WebSocket
            messagingTemplate.convertAndSend("/topic/performance/" + test.getId(), test);

            log.info("Completed performance test: {}", test.getId());

        } catch (Exception e) {
            log.error("Performance test failed", e);
            test.setStatus("FAILED");
            test.setCompletedAt(LocalDateTime.now());
        }
    }

    private PerformanceResults simulateTestExecution(PerformanceTest test) {
        // This is a simulation. In real implementation, integrate with Karate Gatling
        try {
            Thread.sleep(test.getDurationSeconds() * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        PerformanceResults results = new PerformanceResults();

        // Generate realistic-looking stats
        int totalRequests = test.getUsers() * test.getDurationSeconds() * 10;
        int failedRequests = (int) (totalRequests * 0.02); // 2% failure rate

        results.setTotalRequests(totalRequests);
        results.setSuccessfulRequests(totalRequests - failedRequests);
        results.setFailedRequests(failedRequests);
        results.setRequestsPerSecond((double) totalRequests / test.getDurationSeconds());

        results.setMinResponseTime(10);
        results.setMaxResponseTime(500);
        results.setAvgResponseTime(50.5);
        results.setP50ResponseTime(45.0);
        results.setP95ResponseTime(120.0);
        results.setP99ResponseTime(250.0);

        // Status code distribution
        Map<Integer, Integer> statusCodes = new HashMap<>();
        statusCodes.put(200, totalRequests - failedRequests - 50);
        statusCodes.put(201, 50);
        statusCodes.put(400, failedRequests / 2);
        statusCodes.put(500, failedRequests / 2);
        results.setStatusCodeDistribution(statusCodes);

        // Scenario stats
        Map<String, ScenarioStats> scenarioStats = new HashMap<>();
        ScenarioStats stats1 = new ScenarioStats();
        stats1.setName("GET /api/users");
        stats1.setCount(totalRequests / 2);
        stats1.setSuccess(totalRequests / 2 - failedRequests / 2);
        stats1.setFailed(failedRequests / 2);
        stats1.setAvgDuration(45.2);
        stats1.setMinDuration(10);
        stats1.setMaxDuration(300);
        scenarioStats.put("GET /api/users", stats1);

        ScenarioStats stats2 = new ScenarioStats();
        stats2.setName("POST /api/users");
        stats2.setCount(totalRequests / 2);
        stats2.setSuccess(totalRequests / 2 - failedRequests / 2);
        stats2.setFailed(failedRequests / 2);
        stats2.setAvgDuration(55.8);
        stats2.setMinDuration(15);
        stats2.setMaxDuration(500);
        scenarioStats.put("POST /api/users", stats2);

        results.setScenarioStats(scenarioStats);

        return results;
    }

    public List<PerformanceTest> getAllTests() {
        return new ArrayList<>(tests.values());
    }

    public PerformanceTest getTest(String testId) {
        return tests.get(testId);
    }

    public void deleteTest(String testId) {
        tests.remove(testId);
    }

    public String generateGatlingSimulation(String featureFile, String simulationName) {
        // Generate Scala code for Karate Gatling simulation
        StringBuilder scala = new StringBuilder();

        scala.append("package perftest\n\n");
        scala.append("import com.intuit.karate.gatling.PreDef._\n");
        scala.append("import io.gatling.core.Predef._\n");
        scala.append("import scala.concurrent.duration._\n\n");

        String className = simulationName.replaceAll("[^a-zA-Z0-9]", "");
        scala.append("class ").append(className).append("Simulation extends Simulation {\n\n");

        scala.append("  val protocol = karateProtocol()\n\n");

        scala.append("  val ").append(className.toLowerCase()).append(" = scenario(\"").append(simulationName).append("\")\n");
        scala.append("    .exec(karateFeature(\"classpath:").append(featureFile).append("\"))\n\n");

        scala.append("  setUp(\n");
        scala.append("    ").append(className.toLowerCase()).append(".inject(\n");
        scala.append("      rampUsers(10) during (10 seconds)\n");
        scala.append("    ).protocols(protocol)\n");
        scala.append("  )\n");
        scala.append("}\n");

        return scala.toString();
    }
}
