package com.example.dependencies.analyzer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dependencies")
@CrossOrigin(origins = "*")
public class DependencyAnalysisController {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_ANALYSIS_FILE = "dependencies-analysis.json";
    
    @GetMapping("/data")
    public ResponseEntity<Map<String, Object>> getAnalysisData() {
        File analysisFile = new File(DEFAULT_ANALYSIS_FILE);
        
        if (!analysisFile.exists()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "解析結果ファイルが見つかりません。先に解析を実行してください。");
            return ResponseEntity.notFound().build();
        }
        
        try {
            // Read the default analysis file
            Map<String, Object> analysisData = objectMapper.readValue(
                analysisFile, 
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class)
            );
            
            return ResponseEntity.ok(analysisData);
        } catch (IOException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
    
    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadAnalysisResult(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ファイルが選択されていません");
            return ResponseEntity.badRequest().body(error);
        }
        
        try {
            // Parse the uploaded JSON file
            Map<String, Object> analysisData = objectMapper.readValue(
                file.getInputStream(), 
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class)
            );
            
            // Validate the data structure
            if (!analysisData.containsKey("nodes") || !analysisData.containsKey("links")) {
                Map<String, Object> error = new HashMap<>();
                error.put("error", "無効な解析結果ファイルです");
                return ResponseEntity.badRequest().body(error);
            }
            
            return ResponseEntity.ok(analysisData);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "ファイルの読み込みに失敗しました: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}