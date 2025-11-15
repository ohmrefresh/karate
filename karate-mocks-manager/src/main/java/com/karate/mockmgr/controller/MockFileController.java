package com.karate.mockmgr.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class MockFileController {

    private static final String MOCKS_DIR = "mocks";

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Path mocksPath = Paths.get(MOCKS_DIR);
            if (!Files.exists(mocksPath)) {
                Files.createDirectories(mocksPath);
            }

            String filename = file.getOriginalFilename();
            Path filePath = mocksPath.resolve(filename);
            file.transferTo(filePath.toFile());

            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("path", filePath.toString());

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to upload file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listFiles() {
        try {
            Path mocksPath = Paths.get(MOCKS_DIR);
            if (!Files.exists(mocksPath)) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<Map<String, Object>> files = Files.list(mocksPath)
                    .filter(path -> path.toString().endsWith(".feature"))
                    .map(path -> {
                        Map<String, Object> fileInfo = new HashMap<>();
                        fileInfo.put("name", path.getFileName().toString());
                        fileInfo.put("path", path.toString());
                        try {
                            fileInfo.put("size", Files.size(path));
                            fileInfo.put("modified", Files.getLastModifiedTime(path).toString());
                        } catch (IOException e) {
                            // Ignore
                        }
                        return fileInfo;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(files);
        } catch (IOException e) {
            log.error("Failed to list files", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/{filename}")
    public ResponseEntity<Map<String, String>> getFileContent(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(MOCKS_DIR, filename);
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String content = Files.readString(filePath);
            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("content", content);

            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Failed to read file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PutMapping("/{filename}")
    public ResponseEntity<Void> updateFileContent(
            @PathVariable String filename,
            @RequestBody Map<String, String> payload) {
        try {
            Path filePath = Paths.get(MOCKS_DIR, filename);
            Files.writeString(filePath, payload.get("content"));
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to update file", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{filename}")
    public ResponseEntity<Void> deleteFile(@PathVariable String filename) {
        try {
            Path filePath = Paths.get(MOCKS_DIR, filename);
            Files.deleteIfExists(filePath);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            log.error("Failed to delete file", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
