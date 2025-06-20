package com.example.dependencies.analyzer.cli;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.analyzer.DuplicateProjectHandler;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import com.example.dependencies.analyzer.parser.GradleBuildParser;
import com.example.dependencies.analyzer.parser.MavenPomParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyAnalyzerCLI {
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzerCLI.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final MavenPomParser mavenParser = new MavenPomParser();
    private final GradleBuildParser gradleParser = new GradleBuildParser();
    
    static {
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: java -jar dependencies-analyzer.jar <directory-path>");
            System.exit(1);
        }
        
        String directoryPath = args[0];
        DependencyAnalyzerCLI analyzer = new DependencyAnalyzerCLI();
        
        try {
            analyzer.analyzeDependencies(directoryPath);
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies", e);
            System.exit(1);
        }
    }
    
    public void analyzeDependencies(String directoryPath) throws IOException {
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
        
        // Generate and save analysis result
        Map<String, Object> analysisResult = generateAnalysisResult(allProjects, inHouseDependencies);
        saveAnalysisResult(analysisResult);
        
        // Print summary
        printSummary(analysisResult);
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
                    logger.warn("Failed to parse POM file: " + pomFile + ", creating placeholder", e);
                    // Create a placeholder project for failed parsing
                    Project placeholder = createPlaceholderProject(pomFile, ProjectType.MAVEN);
                    projects.add(placeholder);
                }
            }
            
            // Find and parse Gradle projects
            List<Path> gradleFiles = findGradleFiles(repositoryPath);
            for (Path gradleFile : gradleFiles) {
                try {
                    Project project = gradleParser.parse(gradleFile);
                    projects.add(project);
                } catch (Exception e) {
                    logger.warn("Failed to parse Gradle file: " + gradleFile + ", creating placeholder", e);
                    // Create a placeholder project for failed parsing
                    Project placeholder = createPlaceholderProject(gradleFile, ProjectType.GRADLE);
                    projects.add(placeholder);
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
                .collect(Collectors.toList());
        }
    }
    
    private Project createPlaceholderProject(Path buildFile, ProjectType type) {
        String fileName = buildFile.getFileName().toString();
        Path projectDir = buildFile.getParent();
        String artifactId = projectDir.getFileName().toString();
        
        // Try to guess group ID from parent directories
        String groupId = "unknown.group";
        if (projectDir.getParent() != null) {
            String parentName = projectDir.getParent().getFileName().toString();
            if (!parentName.equals("test-projects")) {
                groupId = "com.example." + parentName;
            }
        }
        
        Project project = new Project(groupId, artifactId, "unknown", buildFile, type);
        
        // Set packaging based on file type
        if (type == ProjectType.MAVEN) {
            project.setPackaging("jar"); // Default for Maven
        } else if (type == ProjectType.GRADLE) {
            project.setPackaging("jar"); // Default for Gradle
        }
        
        logger.warn("Created placeholder project for: {} - {}", buildFile, project.getFullName());
        return project;
    }
    
    private Map<String, Object> generateAnalysisResult(List<Project> allProjects, Map<Project, List<Dependency>> dependencyMap) {
        Map<String, Object> result = new HashMap<>();
        
        // Add metadata
        result.put("analysisDate", new Date().toString());
        result.put("version", "1.0");
        
        // Generate graph data
        Map<String, Object> graphData = generateGraphData(allProjects, dependencyMap);
        result.putAll(graphData);
        
        return result;
    }
    
    private Map<String, Object> generateGraphData(List<Project> allProjects, Map<Project, List<Dependency>> dependencyMap) {
        Map<String, Object> result = new HashMap<>();
        ArrayNode nodes = mapper.createArrayNode();
        ArrayNode links = mapper.createArrayNode();
        
        // Use DuplicateProjectHandler to manage unique IDs
        DuplicateProjectHandler duplicateHandler = new DuplicateProjectHandler();
        duplicateHandler.processDuplicates(allProjects);
        
        // Create nodes with unique IDs
        for (Project project : allProjects) {
            String uniqueId = duplicateHandler.getUniqueId(project);
            
            // Debug logging for duplicate projects
            if (project.getGroupId() != null && 
                project.getGroupId().equals("com.example.config") && 
                project.getArtifactId() != null &&
                project.getArtifactId().equals("config-service")) {
                logger.info("Creating node for config-service: uniqueId={}, repository={}", 
                    uniqueId, project.getRepository());
            }
            
            ObjectNode node = mapper.createObjectNode();
            node.put("id", uniqueId);
            node.put("name", project.getArtifactId() != null ? project.getArtifactId() : "");
            node.put("version", project.getVersion() != null ? project.getVersion() : "");
            node.put("group", project.getGroupId());
            node.put("type", project.getType() != null ? project.getType().getDisplayName() : "Unknown");
            node.put("packaging", project.getPackaging() != null ? project.getPackaging() : "jar");
            
            // Determine node group for coloring
            String nodeGroup = "default";
            if (project.getProjectPath() != null) {
                Path repoPath = project.getProjectPath();
                // Find the git repository root
                while (repoPath != null && !Files.exists(repoPath.resolve(".git"))) {
                    repoPath = repoPath.getParent();
                }
                if (repoPath != null) {
                    nodeGroup = repoPath.getFileName().toString();
                }
            }
            node.put("nodeGroup", nodeGroup);
            
            nodes.add(node);
        }
        
        // Create links
        boolean linkToAllDuplicates = false; // Set to true to create links to all duplicate projects
        
        for (Map.Entry<Project, List<Dependency>> entry : dependencyMap.entrySet()) {
            Project source = entry.getKey();
            String sourceId = duplicateHandler.getUniqueId(source);
            
            for (Dependency dep : entry.getValue()) {
                if (linkToAllDuplicates) {
                    // Create links to all matching projects
                    List<String> targetIds = duplicateHandler.resolveAllDependencyIds(dep);
                    for (String targetId : targetIds) {
                        ObjectNode link = mapper.createObjectNode();
                        link.put("source", sourceId);
                        link.put("target", targetId);
                        link.put("value", 1);
                        links.add(link);
                    }
                } else {
                    // Use DuplicateProjectHandler to resolve dependency to correct project
                    String targetId = duplicateHandler.resolveDependencyId(dep, source);
                    
                    // Check if the target project exists
                    if (duplicateHandler.getProjectByUniqueId(targetId) != null) {
                        ObjectNode link = mapper.createObjectNode();
                        link.put("source", sourceId);
                        link.put("target", targetId);
                        link.put("value", 1);
                        links.add(link);
                    }
                }
            }
        }
        
        result.put("nodes", nodes);
        result.put("links", links);
        
        // Add statistics
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProjects", allProjects.size());
        stats.put("totalDependencies", links.size());
        result.put("stats", stats);
        
        // Validate the generated data before returning
        validateGraphData(nodes, links);
        
        return result;
    }
    
    private void validateGraphData(ArrayNode nodes, ArrayNode links) {
        logger.info("Validating graph data...");
        
        // Track validation issues
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        // Create node ID set for link validation
        Set<String> nodeIds = new HashSet<>();
        Map<String, Integer> nodeIndexMap = new HashMap<>();
        
        // Validate nodes
        for (int i = 0; i < nodes.size(); i++) {
            JsonNode node = nodes.get(i);
            String nodeId = node.get("id") != null ? node.get("id").asText() : null;
            
            // Check required fields
            if (nodeId == null || nodeId.isEmpty()) {
                errors.add(String.format("Node at index %d has no id", i));
            } else {
                if (nodeIds.contains(nodeId)) {
                    warnings.add(String.format("Duplicate node id: %s", nodeId));
                }
                nodeIds.add(nodeId);
                nodeIndexMap.put(nodeId, i);
            }
            
            if (node.get("name") == null || node.get("name").asText().isEmpty()) {
                errors.add(String.format("Node %s has no name", nodeId != null ? nodeId : "at index " + i));
            }
            
            if (node.get("version") == null || node.get("version").asText().isEmpty()) {
                warnings.add(String.format("Node %s has no version", nodeId != null ? nodeId : "at index " + i));
            }
            
            if (node.get("group") != null && node.get("group").isNull()) {
                warnings.add(String.format("Node %s has null group", nodeId != null ? nodeId : "at index " + i));
            }
            
            if (node.get("nodeGroup") == null || node.get("nodeGroup").asText().isEmpty()) {
                errors.add(String.format("Node %s has no nodeGroup", nodeId != null ? nodeId : "at index " + i));
            }
        }
        
        // Validate links
        for (int i = 0; i < links.size(); i++) {
            JsonNode link = links.get(i);
            String source = link.get("source") != null ? link.get("source").asText() : null;
            String target = link.get("target") != null ? link.get("target").asText() : null;
            
            if (source == null || source.isEmpty()) {
                errors.add(String.format("Link at index %d has no source", i));
            } else if (!nodeIds.contains(source)) {
                errors.add(String.format("Link at index %d has invalid source: %s", i, source));
            }
            
            if (target == null || target.isEmpty()) {
                errors.add(String.format("Link at index %d has no target", i));
            } else if (!nodeIds.contains(target)) {
                errors.add(String.format("Link at index %d has invalid target: %s", i, target));
            }
            
            // Check for self-referencing links
            if (source != null && source.equals(target)) {
                warnings.add(String.format("Link at index %d is self-referencing: %s -> %s", i, source, target));
            }
        }
        
        // Log validation results
        logger.info("Validation complete: {} nodes, {} links", nodes.size(), links.size());
        
        if (!warnings.isEmpty()) {
            logger.warn("Validation warnings ({}):", warnings.size());
            for (String warning : warnings) {
                logger.warn("  - {}", warning);
            }
        }
        
        if (!errors.isEmpty()) {
            logger.error("Validation errors ({}):", errors.size());
            for (String error : errors) {
                logger.error("  - {}", error);
            }
            throw new IllegalStateException(
                String.format("Graph data validation failed with %d errors. See logs for details.", errors.size())
            );
        }
        
        logger.info("Graph data validation passed successfully");
    }
    
    private void saveAnalysisResult(Map<String, Object> analysisResult) throws IOException {
        File outputFile = new File("dependencies-analysis.json");
        mapper.writeValue(outputFile, analysisResult);
        logger.info("Analysis result saved to: {}", outputFile.getAbsolutePath());
    }
    
    private void printSummary(Map<String, Object> analysisResult) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) analysisResult.get("stats");
        
        System.out.println("\n=== Analysis Summary ===");
        System.out.println("Total projects found: " + stats.get("totalProjects"));
        System.out.println("Total in-house dependencies: " + stats.get("totalDependencies"));
        System.out.println("\nAnalysis result saved to: dependencies-analysis.json");
        System.out.println("\nTo visualize the results:");
        System.out.println("1. Run: mvn spring-boot:run");
        System.out.println("2. Open: http://localhost:8080");
        System.out.println("3. Upload the dependencies-analysis.json file");
    }
}