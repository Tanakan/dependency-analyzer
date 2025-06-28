package com.example.dependencies.analyzer;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.analyzer.DuplicateProjectHandler;
import com.example.dependencies.analyzer.analyzer.ProjectIssuesAnalyzer;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import com.example.dependencies.analyzer.parser.GradleBuildParser;
import com.example.dependencies.analyzer.parser.MavenPomParser;
import com.example.dependencies.analyzer.visualizer.DependencyGraphVisualizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.net.URI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.util.HashMap;

public class DependencyAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(DependencyAnalyzer.class);
    
    private final MavenPomParser mavenParser = new MavenPomParser();
    private final GradleBuildParser gradleParser = new GradleBuildParser();
    
    public void analyze(String directoryPath) throws IOException {
        Path rootPath = Paths.get(directoryPath);
        
        if (!Files.exists(rootPath) || !Files.isDirectory(rootPath)) {
            throw new IllegalArgumentException("Invalid directory path: " + directoryPath);
        }
        
        logger.info("Scanning for Git repositories in: {}", directoryPath);
        
        List<Path> gitRepositories = findGitRepositories(rootPath);
        logger.info("Found {} Git repositories", gitRepositories.size());
        
        // Collect all projects
        List<Project> allProjects = new ArrayList<>();
        
        // If no git repositories found, analyze the root directory itself
        if (gitRepositories.isEmpty()) {
            logger.info("No Git repositories found, analyzing directory as a single repository");
            allProjects = analyzeRepository(rootPath);
        } else {
            for (Path repo : gitRepositories) {
                List<Project> repoProjects = analyzeRepository(repo);
                allProjects.addAll(repoProjects);
            }
        }
        
        logger.info("Total projects found: {}", allProjects.size());
        
        // Detect in-house dependencies
        InHouseProjectDetector detector = new InHouseProjectDetector(allProjects);
        Map<Project, List<Dependency>> inHouseDependencies = detector.buildInHouseDependencyMap();
        
        // Visualize dependencies - Console output
        DependencyGraphVisualizer textVisualizer = new DependencyGraphVisualizer();
        String visualization = textVisualizer.visualize(inHouseDependencies);
        System.out.println(visualization);
        
        // Save analysis data as JSON
        try {
            saveAnalysisDataAsJson(inHouseDependencies, allProjects);
        } catch (IOException e) {
            logger.error("Failed to save analysis data as JSON", e);
        }
        
        // HTML visualization is handled by static HTML files
        logger.info("Analysis complete. Open src/main/resources/static/simple-graph.html in your browser.");
    }
    
    public List<Path> findGitRepositories(Path rootPath) throws IOException {
        try (Stream<Path> paths = Files.walk(rootPath)) {
            return paths
                .filter(Files::isDirectory)
                .filter(path -> Files.exists(path.resolve(".git")))
                .collect(Collectors.toList());
        }
    }
    
    public List<Project> analyzeRepository(Path repositoryPath) {
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
            groupId = "com.example." + parentName;
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
    
    private void saveAnalysisDataAsJson(Map<Project, List<Dependency>> inHouseDependencies, List<Project> allProjects) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        
        // Use DuplicateProjectHandler to manage unique IDs
        DuplicateProjectHandler duplicateHandler = new DuplicateProjectHandler();
        duplicateHandler.processDuplicates(allProjects);
        
        // Create nodes array with unique IDs for duplicates
        ArrayNode nodes = mapper.createArrayNode();
        
        for (Project project : allProjects) {
            String uniqueId = duplicateHandler.getUniqueId(project);
            
            ObjectNode node = mapper.createObjectNode();
            node.put("id", uniqueId);
            node.put("name", project.getArtifactId());
            node.put("version", project.getVersion());
            node.put("group", project.getGroupId());
            node.put("type", project.getType().toString());
            node.put("packaging", project.getPackaging());
            
            // Determine repository from path
            String repository = determineRepository(project);
            node.put("nodeGroup", repository);
            
            nodes.add(node);
        }
        
        // Create links array using unique IDs
        ArrayNode links = mapper.createArrayNode();
        
        for (Map.Entry<Project, List<Dependency>> entry : inHouseDependencies.entrySet()) {
            Project source = entry.getKey();
            String sourceId = duplicateHandler.getUniqueId(source);
            
            for (Dependency dep : entry.getValue()) {
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
        
        // Add to root
        root.set("nodes", nodes);
        root.set("links", links);
        
        // Add statistics
        ObjectNode stats = mapper.createObjectNode();
        stats.put("totalProjects", allProjects.size());
        stats.put("totalDependencies", links.size());
        root.set("stats", stats);
        
        // Add issues analysis
        ProjectIssuesAnalyzer issuesAnalyzer = new ProjectIssuesAnalyzer(allProjects, inHouseDependencies);
        ProjectIssuesAnalyzer.IssuesReport issuesReport = issuesAnalyzer.analyzeAll();
        
        ObjectNode issues = mapper.createObjectNode();
        
        // Circular references
        ArrayNode circularRefs = mapper.createArrayNode();
        for (List<String> cycle : issuesReport.getCircularReferences()) {
            ArrayNode cycleArray = mapper.createArrayNode();
            cycle.forEach(cycleArray::add);
            circularRefs.add(cycleArray);
        }
        issues.set("circularReferences", circularRefs);
        
        // Unreferenced projects
        ArrayNode unreferencedProjects = mapper.createArrayNode();
        for (Project project : issuesReport.getUnreferencedProjects()) {
            unreferencedProjects.add(duplicateHandler.getUniqueId(project));
        }
        issues.set("unreferencedProjects", unreferencedProjects);
        
        // Duplicate artifact IDs
        ObjectNode duplicateArtifactIds = mapper.createObjectNode();
        for (Map.Entry<String, List<Project>> entry : issuesReport.getDuplicateArtifactIds().entrySet()) {
            ArrayNode projectIds = mapper.createArrayNode();
            for (Project project : entry.getValue()) {
                projectIds.add(duplicateHandler.getUniqueId(project));
            }
            duplicateArtifactIds.set(entry.getKey(), projectIds);
        }
        issues.set("duplicateArtifactIds", duplicateArtifactIds);
        
        // Duplicate GAVs
        ObjectNode duplicateGAVs = mapper.createObjectNode();
        for (Map.Entry<String, List<Project>> entry : issuesReport.getDuplicateGAVs().entrySet()) {
            ArrayNode projectIds = mapper.createArrayNode();
            for (Project project : entry.getValue()) {
                projectIds.add(duplicateHandler.getUniqueId(project));
            }
            duplicateGAVs.set(entry.getKey(), projectIds);
        }
        issues.set("duplicateGAVs", duplicateGAVs);
        
        root.set("issues", issues);
        
        // Add metadata
        root.put("analysisDate", new java.util.Date().toString());
        root.put("version", "1.0");
        
        // Write to file
        mapper.writerWithDefaultPrettyPrinter().writeValue(
            Files.newOutputStream(Paths.get("dependencies-analysis.json")), 
            root
        );
        logger.info("Analysis data saved to dependencies-analysis.json");
    }
    
    private String determineRepository(Project project) {
        Path projectPath = project.getProjectPath();
        if (projectPath == null) return "unknown";
        
        try {
            // Find the git repository root
            Path currentPath = projectPath;
            while (currentPath != null && !Files.exists(currentPath.resolve(".git"))) {
                currentPath = currentPath.getParent();
            }
            
            if (currentPath != null) {
                // Return the repository directory name
                return currentPath.getFileName().toString();
            }
            
            // If no git repository found, use the project directory name
            return projectPath.getFileName().toString();
        } catch (Exception e) {
            logger.warn("Failed to determine repository for project: " + project.getArtifactId(), e);
            return "unknown";
        }
    }
}