package com.karate.mockmgr.controller;

import com.karate.mockmgr.service.OpenApiImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/openapi")
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiImportService openApiService;
    private static final String MOCKS_DIR = "mocks";

    @PostMapping("/import")
    public ResponseEntity<Map<String, String>> importOpenApi(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "featureName", required = false) String featureName) {

        try {
            String content = new String(file.getBytes());
            String karateFeature = openApiService.convertToKarateFeature(content, featureName);

            // Save to file
            String filename = (featureName != null ? featureName : "imported") + ".feature";
            Path filePath = Paths.get(MOCKS_DIR, filename);
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, karateFeature);

            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("path", filePath.toString());
            response.put("content", karateFeature);

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/convert")
    public ResponseEntity<Map<String, String>> convertOpenApi(@RequestBody Map<String, String> payload) {
        try {
            String openApiContent = payload.get("content");
            String featureName = payload.get("featureName");

            String karateFeature = openApiService.convertToKarateFeature(openApiContent, featureName);

            Map<String, String> response = new HashMap<>();
            response.put("content", karateFeature);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }
}
