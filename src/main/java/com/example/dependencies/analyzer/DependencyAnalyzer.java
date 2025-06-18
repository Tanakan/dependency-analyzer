package com.example.dependencies.analyzer;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import com.example.dependencies.analyzer.parser.GradleBuildParser;
import com.example.dependencies.analyzer.parser.MavenPomParser;
import com.example.dependencies.analyzer.visualizer.DependencyGraphVisualizer;
import com.example.dependencies.analyzer.visualizer.HtmlGraphVisualizer;
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
import java.awt.Desktop;
import java.net.URI;

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
        
        for (Path repo : gitRepositories) {
            List<Project> repoProjects = analyzeRepository(repo);
            allProjects.addAll(repoProjects);
        }
        
        logger.info("Total projects found: {}", allProjects.size());
        
        // Detect in-house dependencies
        InHouseProjectDetector detector = new InHouseProjectDetector(allProjects);
        Map<Project, List<Dependency>> inHouseDependencies = detector.buildInHouseDependencyMap();
        
        // Visualize dependencies - Console output
        DependencyGraphVisualizer textVisualizer = new DependencyGraphVisualizer();
        String visualization = textVisualizer.visualize(inHouseDependencies);
        System.out.println(visualization);
        
        // Generate HTML visualization
        HtmlGraphVisualizer htmlVisualizer = new HtmlGraphVisualizer();
        Path htmlOutputPath = Paths.get("dependency-graph.html");
        try {
            htmlVisualizer.generateHtmlVisualization(inHouseDependencies, htmlOutputPath);
            logger.info("HTML visualization generated: {}", htmlOutputPath.toAbsolutePath());
            System.out.println("\nHTML visualization saved to: " + htmlOutputPath.toAbsolutePath());
            
            // Try to open in browser
            openInBrowser(htmlOutputPath);
        } catch (IOException e) {
            logger.error("Failed to generate HTML visualization", e);
        }
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
    
    private void openInBrowser(Path htmlPath) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(htmlPath.toAbsolutePath().toUri());
                logger.info("Opened HTML visualization in browser");
            } else {
                logger.warn("Desktop browsing not supported on this platform");
            }
        } catch (Exception e) {
            logger.warn("Could not open browser automatically: " + e.getMessage());
        }
    }
}