package com.example.dependencies.analyzer.service;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.parser.GradleBuildParser;
import com.example.dependencies.analyzer.parser.MavenPomParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class DependencyAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalysisService.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final MavenPomParser mavenParser = new MavenPomParser();
    private final GradleBuildParser gradleParser = new GradleBuildParser();
    
    public Map<String, Object> analyzeDependencies(String directoryPath) throws IOException {
        Path rootPath = Paths.get(directoryPath);
        
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }
        
        logger.info("Scanning for Git repositories in: {}", directoryPath);
        
        List<Path> gitRepositories = findGitRepositories(rootPath);
        logger.info("Found {} Git repositories", gitRepositories.size());
        
        // Collect all projects
        List<Project> allProjects = new ArrayList<>();
        
        for (Path repo : gitRepositories) {
            List<Project> repoProjects = analyzeRepository(repo);
            allProjects.addAll(repoProjects);
        }
        
        logger.info("Total projects found: {}", allProjects.size());
        
        // Detect in-house dependencies
        InHouseProjectDetector detector = new InHouseProjectDetector(allProjects);
        Map<Project, List<Dependency>> inHouseDependencies = detector.buildInHouseDependencyMap();
        
        // Generate graph data
        return generateGraphData(inHouseDependencies);
    }
    
    private List<Path> findGitRepositories(Path rootPath) throws IOException {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            return paths
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve(".git")))
                .collect(Collectors.toList());
        }
    }
    
    private List<Project> analyzeRepository(Path repositoryPath) {
        logger.info("Analyzing repository: {}", repositoryPath);
        List<Project> projects = new ArrayList<>();
        
        try {
            // Find and parse Maven projects
            List<Path> pomFiles = findPomFiles(repositoryPath);
            for (Path pomFile : pomFiles) {
                try {
                    Project project = mavenParser.parse(pomFile);
                    projects.add(project);
                } catch (Exception e) {
                    logger.error("Failed to parse POM file: " + pomFile, e);
                }
            }
            
            // Find and parse Gradle projects
            List<Path> gradleFiles = findGradleFiles(repositoryPath);
            for (Path gradleFile : gradleFiles) {
                try {
                    Project project = gradleParser.parse(gradleFile);
                    projects.add(project);
                } catch (Exception e) {
                    logger.error("Failed to parse Gradle file: " + gradleFile, e);
                }
            }
            
            logger.info("  Found {} Maven projects", pomFiles.size());
            logger.info("  Found {} Gradle projects", gradleFiles.size());
            
        } catch (IOException e) {
            logger.error("Error analyzing repository: " + repositoryPath, e);
        }
        
        return projects;
    }
    
    private List<Path> findPomFiles(Path repositoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(repositoryPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().equals("pom.xml"))
                .collect(Collectors.toList());
        }
    }
    
    private List<Path> findGradleFiles(Path repositoryPath) throws IOException {
        try (Stream<Path> paths = Files.walk(repositoryPath)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(path -> {
                    String fileName = path.getFileName().toString();
                    return fileName.equals("build.gradle") || fileName.equals("build.gradle.kts");
                })
                .filter(path -> {
                    // Skip if there's a pom.xml in the same directory (avoid duplicates)
                    return !Files.exists(path.getParent().resolve("pom.xml"));
                })
                .collect(Collectors.toList());
        }
    }
    
    private Map<String, Object> generateGraphData(Map<Project, List<Dependency>> dependencyMap) {
        Map<String, Object> result = new HashMap<>();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode links = mapper.createArrayNode();
        
        // Create a map to track node indices
        Map<String, Integer> nodeIndices = new HashMap<>();
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
            nodeIndices.put(key, i);
            
            ObjectNode node = mapper.createObjectNode();
            node.put("id", key);
            node.put("name", project.getArtifactId());
            node.put("version", project.getVersion());
            node.put("group", project.getGroupId());
            node.put("type", project.getType() != null ? project.getType().getDisplayName() : "Unknown");
            node.put("packaging", project.getPackaging());
            
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
            
            for (Dependency dep : entry.getValue()) {
                String targetKey = dep.getGroupId() + ":" + dep.getArtifactId();
                
                // Use node IDs instead of indices for D3.js
                ObjectNode link = mapper.createObjectNode();
                link.put("source", sourceKey);
                link.put("target", targetKey);
                link.put("value", 1);
                links.add(link);
            }
        }
        
        result.put("nodes", nodes);
        result.put("links", links);
        
        // Add statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", allProjects.size());
        stats.put("totalDependencies", links.size());
        result.put("stats", stats);
        
        return result;
    }
}