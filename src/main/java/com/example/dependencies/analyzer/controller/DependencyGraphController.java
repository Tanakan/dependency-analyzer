package com.example.dependencies.analyzer.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class DependencyGraphController {

    @GetMapping(value = "/api/dependencies-analysis", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getDependenciesAnalysis() throws IOException {
        // First try to load from file system
        Path analysisFile = Paths.get("dependencies-analysis.json");
        if (Files.exists(analysisFile)) {
            String content = Files.readString(analysisFile);
            return ResponseEntity.ok(content);
        }
        
        // Fallback to empty response
        return ResponseEntity.ok("{}");
    }
}