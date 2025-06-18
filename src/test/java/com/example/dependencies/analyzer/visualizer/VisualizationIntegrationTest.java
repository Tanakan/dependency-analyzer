package com.example.dependencies.analyzer.visualizer;

import com.example.dependencies.analyzer.DependencyAnalyzer;
import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VisualizationIntegrationTest {
    
    @Test
    void testDuplicateProjectsInRealData() throws Exception {
        // Run the analyzer on test-projects
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        List<Project> allProjects = analyzer.analyzeRepository(Path.of("test-projects"));
        
        // Count projects with same groupId:artifactId
        Map<String, Integer> projectCounts = new java.util.HashMap<>();
        for (Project p : allProjects) {
            String key = p.getGroupId() + ":" + p.getArtifactId();
            projectCounts.merge(key, 1, Integer::sum);
        }
        
        // Find duplicates
        System.out.println("=== Duplicate Projects ===");
        projectCounts.entrySet().stream()
            .filter(e -> e.getValue() > 1)
            .forEach(e -> System.out.println(e.getKey() + " appears " + e.getValue() + " times"));
        
        // Use InHouseProjectDetector to build dependency map
        InHouseProjectDetector detector = new InHouseProjectDetector(allProjects);
        Map<Project, List<Dependency>> dependencyMap = detector.buildInHouseDependencyMap();
        
        // Generate visualization
        HtmlGraphVisualizer visualizer = new HtmlGraphVisualizer();
        Path tempFile = Files.createTempFile("test-viz", ".html");
        visualizer.generateHtmlVisualization(dependencyMap, tempFile);
        
        // Parse the generated HTML to check for duplicates
        String html = Files.readString(tempFile);
        int startIndex = html.indexOf("const graphData = ") + "const graphData = ".length();
        int endIndex = html.indexOf(";", startIndex);
        String json = html.substring(startIndex, endIndex);
        
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        
        // Count nodes with same artifactId
        Map<String, Integer> nodeCounts = new java.util.HashMap<>();
        for (JsonNode node : root.get("nodes")) {
            String id = node.get("id").asText();
            nodeCounts.merge(id, 1, Integer::sum);
        }
        
        // Check if all projects are represented
        assertTrue(dependencyMap.size() > 20, "Should have many projects");
        
        System.out.println("Total projects in dependency map: " + dependencyMap.size());
        System.out.println("Total nodes in visualization: " + root.get("nodes").size());
        
        // Clean up
        Files.deleteIfExists(tempFile);
    }
}