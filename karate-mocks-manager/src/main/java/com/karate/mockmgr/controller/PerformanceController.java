package com.karate.mockmgr.controller;

import com.karate.mockmgr.model.PerformanceTest;
import com.karate.mockmgr.service.PerformanceTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/performance")
@RequiredArgsConstructor
public class PerformanceController {

    private final PerformanceTestService performanceService;

    @PostMapping("/tests")
    public ResponseEntity<PerformanceTest> createTest(@RequestBody Map<String, Object> payload) {
        String name = (String) payload.get("name");
        String featureFile = (String) payload.get("featureFile");
        int users = (Integer) payload.getOrDefault("users", 10);
        int rampUpSeconds = (Integer) payload.getOrDefault("rampUpSeconds", 10);
        int durationSeconds = (Integer) payload.getOrDefault("durationSeconds", 60);

        PerformanceTest test = performanceService.createTest(name, featureFile, users, rampUpSeconds, durationSeconds);
        return ResponseEntity.ok(test);
    }

    @PostMapping("/tests/{testId}/start")
    public ResponseEntity<Void> startTest(@PathVariable String testId) {
        performanceService.startTest(testId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/tests")
    public ResponseEntity<List<PerformanceTest>> getAllTests() {
        return ResponseEntity.ok(performanceService.getAllTests());
    }

    @GetMapping("/tests/{testId}")
    public ResponseEntity<PerformanceTest> getTest(@PathVariable String testId) {
        PerformanceTest test = performanceService.getTest(testId);
        if (test == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(test);
    }

    @DeleteMapping("/tests/{testId}")
    public ResponseEntity<Void> deleteTest(@PathVariable String testId) {
        performanceService.deleteTest(testId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/generate-simulation")
    public ResponseEntity<Map<String, String>> generateSimulation(@RequestBody Map<String, String> payload) {
        String featureFile = payload.get("featureFile");
        String simulationName = payload.get("simulationName");

        String scalaCode = performanceService.generateGatlingSimulation(featureFile, simulationName);

        Map<String, String> response = new HashMap<>();
        response.put("simulation", scalaCode);

        return ResponseEntity.ok(response);
    }
}
