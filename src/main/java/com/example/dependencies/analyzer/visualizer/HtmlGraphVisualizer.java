package com.example.dependencies.analyzer.visualizer;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class HtmlGraphVisualizer {
    private static final ObjectMapper mapper = new ObjectMapper();
    
    public void generateHtmlVisualization(Map<Project, List<Dependency>> dependencyMap, Path outputPath) throws IOException {
        // Generate graph data in JSON format
        String graphDataJson = generateGraphDataJson(dependencyMap);
        
        // Load HTML template from resources
        String htmlTemplate = loadHtmlTemplate();
        
        // Replace placeholder with actual data
        String finalHtml = htmlTemplate.replace("__GRAPH_DATA__", graphDataJson);
        
        // Write to file
        Files.writeString(outputPath, finalHtml, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }
    
    private String generateGraphDataJson(Map<Project, List<Dependency>> dependencyMap) {
        ObjectNode root = mapper.createObjectNode();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode links = mapper.createArrayNode();
        
        // Create a map to track node indices - now supports multiple indices per key
        Map<String, List<Integer>> nodeIndicesMap = new HashMap<>();
        List<Project> allProjects = new ArrayList<>();
        
        // First, collect all projects
        for (Project project : dependencyMap.keySet()) {
            allProjects.add(project);
        }
        
        // Add all dependent projects that might not be in the keySet
        for (List<Dependency> deps : dependencyMap.values()) {
            for (Dependency dep : deps) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                boolean found = false;
                for (Project p : allProjects) {
                    if ((p.getGroupId() + ":" + p.getArtifactId()).equals(depKey)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    // Create a placeholder project for dependencies not in our scan
                    Project placeholder = new Project(
                        dep.getGroupId(), 
                        dep.getArtifactId(), 
                        dep.getVersion(), 
                        null, 
                        null
                    );
                    allProjects.add(placeholder);
                }
            }
        }
        
        // Create nodes
        for (int i = 0; i < allProjects.size(); i++) {
            Project project = allProjects.get(i);
            String key = project.getGroupId() + ":" + project.getArtifactId();
            
            // Support multiple nodes with same groupId:artifactId
            nodeIndicesMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            
            ObjectNode node = mapper.createObjectNode();
            node.put("id", key);
            node.put("name", project.getArtifactId());
            node.put("version", project.getVersion());
            node.put("group", project.getGroupId());
            node.put("type", project.getType() != null ? project.getType().getDisplayName() : "Unknown");
            
            // Determine node group for coloring
            String nodeGroup = "default";
            if (project.getProjectPath() != null) {
                String repoName = project.getProjectPath().getParent().getFileName().toString();
                nodeGroup = repoName;
            }
            node.put("nodeGroup", nodeGroup);
            
            nodes.add(node);
        }
        
        // Create links
        for (Map.Entry<Project, List<Dependency>> entry : dependencyMap.entrySet()) {
            Project source = entry.getKey();
            String sourceKey = source.getGroupId() + ":" + source.getArtifactId();
            
            // Find the correct source index by matching the full project
            Integer sourceIndex = null;
            for (int i = 0; i < allProjects.size(); i++) {
                if (allProjects.get(i) == source) {
                    sourceIndex = i;
                    break;
                }
            }
            
            if (sourceIndex == null) continue;
            
            for (Dependency dep : entry.getValue()) {
                String targetKey = dep.getGroupId() + ":" + dep.getArtifactId();
                List<Integer> targetIndices = nodeIndicesMap.get(targetKey);
                
                if (targetIndices != null) {
                    // Create links to ALL nodes with matching groupId:artifactId
                    for (Integer targetIndex : targetIndices) {
                        ObjectNode link = mapper.createObjectNode();
                        link.put("source", sourceIndex);
                        link.put("target", targetIndex);
                        link.put("value", 1);
                        links.add(link);
                    }
                }
            }
        }
        
        root.set("nodes", nodes);
        root.set("links", links);
        
        try {
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON", e);
        }
    }
    
    private String loadHtmlTemplate() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/dependency-graph-template.html")) {
            if (is == null) {
                throw new IOException("HTML template resource not found: /dependency-graph-template.html");
            }
            return new String(is.readAllBytes());
        }
    }
    
}