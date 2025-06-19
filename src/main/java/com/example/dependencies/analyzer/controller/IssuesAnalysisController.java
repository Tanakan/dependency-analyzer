package com.example.dependencies.analyzer.controller;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.analyzer.ProjectIssuesAnalyzer;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/issues")
@CrossOrigin(origins = "*")
public class IssuesAnalysisController {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String DEFAULT_ANALYSIS_FILE = "dependencies-analysis.json";
    
    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> getIssuesAnalysis() {
        // Read projects from the analysis file
        List<Project> allProjects = new ArrayList<>();
        
        try {
            // Read the analysis file and reconstruct projects
            java.io.File analysisFile = new java.io.File(DEFAULT_ANALYSIS_FILE);
            if (!analysisFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> analysisData = objectMapper.readValue(
                analysisFile,
                objectMapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class)
            );
            
            // Extract projects from nodes
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) analysisData.get("nodes");
            Map<String, Project> projectMap = new HashMap<>();
            
            for (Map<String, Object> node : nodes) {
                String id = (String) node.get("id");
                String groupId = (String) node.get("group");
                String artifactId = (String) node.get("name");
                String version = (String) node.get("version");
                String nodeGroup = (String) node.get("nodeGroup");
                
                if (groupId != null && artifactId != null) {
                    Path projectPath = null;
                    if (nodeGroup != null && !"default".equals(nodeGroup)) {
                        projectPath = Path.of("test-projects", nodeGroup);
                    }
                    
                    Project project = new Project(groupId, artifactId, version, projectPath, null);
                    projectMap.put(id, project);
                    allProjects.add(project);
                }
            }
            
            // Build dependency relationships from links
            List<Map<String, Object>> links = (List<Map<String, Object>>) analysisData.get("links");
            for (Map<String, Object> link : links) {
                String sourceId = (String) link.get("source");
                String targetId = (String) link.get("target");
                
                Project source = projectMap.get(sourceId);
                if (source != null && targetId != null) {
                    String[] targetParts = targetId.split(":");
                    if (targetParts.length >= 2) {
                        Project target = projectMap.get(targetId);
                        if (target != null) {
                            source.addDependency(new Dependency(
                                target.getGroupId(),
                                target.getArtifactId(),
                                target.getVersion(),
                                "compile"
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to read analysis data: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
        
        // Build dependency map
        InHouseProjectDetector detector = new InHouseProjectDetector(allProjects);
        Map<Project, List<Dependency>> dependencyMap = detector.buildInHouseDependencyMap();
        
        // Analyze issues
        ProjectIssuesAnalyzer analyzer = new ProjectIssuesAnalyzer(allProjects, dependencyMap);
        ProjectIssuesAnalyzer.IssuesReport report = analyzer.analyzeAll();
        
        // Build response
        Map<String, Object> response = new HashMap<>();
        
        // Circular references
        List<Map<String, Object>> circularRefs = new ArrayList<>();
        for (List<String> cycle : report.getCircularReferences()) {
            Map<String, Object> cycleInfo = new HashMap<>();
            cycleInfo.put("cycle", cycle);
            cycleInfo.put("length", cycle.size() - 1); // Exclude duplicate at end
            circularRefs.add(cycleInfo);
        }
        response.put("circularReferences", circularRefs);
        
        // Unreferenced projects
        List<Map<String, String>> unreferencedList = report.getUnreferencedProjects().stream()
            .map(p -> {
                Map<String, String> info = new HashMap<>();
                info.put("fullName", p.getFullName());
                info.put("groupId", p.getGroupId());
                info.put("artifactId", p.getArtifactId());
                info.put("version", p.getVersion());
                info.put("path", p.getProjectPath() != null ? p.getProjectPath().toString() : "");
                return info;
            })
            .collect(Collectors.toList());
        response.put("unreferencedProjects", unreferencedList);
        
        // Duplicate artifact IDs
        List<Map<String, Object>> duplicates = new ArrayList<>();
        for (Map.Entry<String, List<Project>> entry : report.getDuplicateArtifactIds().entrySet()) {
            Map<String, Object> dupInfo = new HashMap<>();
            dupInfo.put("artifactId", entry.getKey());
            
            List<Map<String, String>> projects = entry.getValue().stream()
                .map(p -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("fullName", p.getFullName());
                    info.put("groupId", p.getGroupId());
                    info.put("version", p.getVersion());
                    info.put("path", p.getProjectPath() != null ? p.getProjectPath().toString() : "");
                    return info;
                })
                .collect(Collectors.toList());
            
            dupInfo.put("projects", projects);
            dupInfo.put("count", projects.size());
            duplicates.add(dupInfo);
        }
        response.put("duplicateArtifactIds", duplicates);
        
        // Duplicate GAVs (same GroupId, ArtifactId, and Version)
        List<Map<String, Object>> duplicateGAVs = new ArrayList<>();
        for (Map.Entry<String, List<Project>> entry : report.getDuplicateGAVs().entrySet()) {
            Map<String, Object> dupInfo = new HashMap<>();
            dupInfo.put("gav", entry.getKey());
            
            List<Map<String, String>> projects = entry.getValue().stream()
                .map(p -> {
                    Map<String, String> info = new HashMap<>();
                    info.put("fullName", p.getFullName());
                    info.put("groupId", p.getGroupId());
                    info.put("artifactId", p.getArtifactId());
                    info.put("version", p.getVersion());
                    info.put("path", p.getProjectPath() != null ? p.getProjectPath().toString() : "");
                    return info;
                })
                .collect(Collectors.toList());
            
            dupInfo.put("projects", projects);
            dupInfo.put("count", projects.size());
            duplicateGAVs.add(dupInfo);
        }
        response.put("duplicateGAVs", duplicateGAVs);
        
        // Summary statistics
        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalProjects", allProjects.size());
        stats.put("circularReferencesCount", circularRefs.size());
        stats.put("unreferencedProjectsCount", unreferencedList.size());
        stats.put("duplicateArtifactIdsCount", duplicates.size());
        stats.put("duplicateGAVsCount", duplicateGAVs.size());
        response.put("statistics", stats);
        
        return ResponseEntity.ok(response);
    }
}