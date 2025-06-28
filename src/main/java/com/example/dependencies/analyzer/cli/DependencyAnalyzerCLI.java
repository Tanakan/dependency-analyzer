package com.example.dependencies.analyzer.cli;

import com.example.dependencies.analyzer.analyzer.InHouseProjectDetector;
import com.example.dependencies.analyzer.analyzer.DuplicateProjectHandler;
import com.example.dependencies.analyzer.analyzer.ProjectIssuesAnalyzer;
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
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DependencyAnalyzerCLI {
    private static final Logger logger;
    private static final ObjectMapper mapper = new ObjectMapper();
    
    private final MavenPomParser mavenParser = new MavenPomParser();
    private final GradleBuildParser gradleParser = new GradleBuildParser();
    
    static {
        // Configure logging for CLI mode BEFORE logger initialization
        configureLoggingLevel();
        
        // Initialize logger after configuration
        logger = LoggerFactory.getLogger(DependencyAnalyzerCLI.class);
        
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }
    
    private static void configureLoggingLevel() {
        // Get log level from system property or environment variable
        String logLevel = System.getProperty("logging.level.root");
        if (logLevel == null) {
            logLevel = System.getenv("LOGGING_LEVEL_ROOT");
        }
        if (logLevel == null) {
            logLevel = "ERROR"; // Default to ERROR for CLI mode
        }
        
        // Get the logger context
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        
        // Reset the context to clear any existing configuration
        loggerContext.reset();
        
        // Now configure programmatically
        ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        
        try {
            Level level = Level.valueOf(logLevel.toUpperCase());
            rootLogger.setLevel(level);
            
            // Also set for our package
            String packageLogLevel = System.getProperty("logging.level.com.example.dependencies.analyzer");
            if (packageLogLevel == null) {
                packageLogLevel = System.getenv("LOGGING_LEVEL_COM_EXAMPLE_DEPENDENCIES_ANALYZER");
            }
            if (packageLogLevel == null) {
                // Default package log level to same as root
                packageLogLevel = logLevel;
            }
            ch.qos.logback.classic.Logger packageLogger = loggerContext.getLogger("com.example.dependencies.analyzer");
            packageLogger.setLevel(Level.valueOf(packageLogLevel.toUpperCase()));
            
            // Add a console appender for ERROR output
            ch.qos.logback.core.ConsoleAppender<ch.qos.logback.classic.spi.ILoggingEvent> consoleAppender = 
                new ch.qos.logback.core.ConsoleAppender<>();
            consoleAppender.setContext(loggerContext);
            consoleAppender.setTarget("System.err");
            
            ch.qos.logback.classic.encoder.PatternLayoutEncoder encoder = 
                new ch.qos.logback.classic.encoder.PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n");
            encoder.start();
            
            consoleAppender.setEncoder(encoder);
            consoleAppender.start();
            
            rootLogger.addAppender(consoleAppender);
            
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid log level: " + logLevel + ". Using ERROR level.");
            rootLogger.setLevel(Level.ERROR);
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 1 || args.length > 2) {
            System.err.println("Usage: java -jar dependencies-analyzer.jar <directory-path> [output-file]");
            System.err.println("Options:");
            System.err.println("  directory-path                  Directory to analyze");
            System.err.println("  output-file                     Output JSON file (default: dependencies-analysis.json)");
            System.err.println("  -Danalyzer.output.directory=DIR Set output directory (default: ./frontend/public)");
            System.err.println("  -Danalyzer.output.filename=FILE Set output filename (default: dependencies-analysis.json)");
            System.err.println("  -Dlogging.level.root=<LEVEL>    Set root log level (TRACE, DEBUG, INFO, WARN, ERROR, OFF)");
            System.err.println("  -Dlogging.level.com.example.dependencies.analyzer=<LEVEL>  Set package log level");
            System.exit(1);
        }
        
        String directoryPath = args[0];
        String outputFile = args.length > 1 ? args[1] : null;
        DependencyAnalyzerCLI analyzer = new DependencyAnalyzerCLI();
        
        try {
            analyzer.analyzeDependencies(directoryPath, outputFile);
        } catch (Exception e) {
            logger.error("Failed to analyze dependencies", e);
            System.exit(1);
        }
    }
    
    public void analyzeDependencies(String directoryPath) throws IOException {
        analyzeDependencies(directoryPath, null);
    }
    
    public void analyzeDependencies(String directoryPath, String outputFile) throws IOException {
        // Expand tilde if present
        if (directoryPath.startsWith("~")) {
            String homeDir = System.getProperty("user.home");
            directoryPath = homeDir + directoryPath.substring(1);
        }
        
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
        saveAnalysisResult(analysisResult, outputFile);
        
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
    
    private Map<String, Object> generateAnalysisResult(List<Project> allProjects, Map<Project, List<Dependency>> dependencyMap) {
        Map<String, Object> result = new HashMap<>();
        
        // Add metadata
        result.put("analysisDate", new Date().toString());
        result.put("version", "1.0");
        
        // Generate graph data
        Map<String, Object> graphData = generateGraphData(allProjects, dependencyMap);
        result.putAll(graphData);
        
        // Add issues analysis
        ProjectIssuesAnalyzer issuesAnalyzer = new ProjectIssuesAnalyzer(allProjects, dependencyMap);
        ProjectIssuesAnalyzer.IssuesReport issuesReport = issuesAnalyzer.analyzeAll();
        
        Map<String, Object> issues = new HashMap<>();
        
        // Circular references
        issues.put("circularReferences", issuesReport.getCircularReferences());
        
        // Unreferenced projects
        List<String> unreferencedProjectIds = new ArrayList<>();
        DuplicateProjectHandler duplicateHandler = new DuplicateProjectHandler();
        duplicateHandler.processDuplicates(allProjects);
        for (Project project : issuesReport.getUnreferencedProjects()) {
            unreferencedProjectIds.add(duplicateHandler.getUniqueId(project));
        }
        issues.put("unreferencedProjects", unreferencedProjectIds);
        
        // Duplicate artifact IDs
        Map<String, List<String>> duplicateArtifactIds = new HashMap<>();
        for (Map.Entry<String, List<Project>> entry : issuesReport.getDuplicateArtifactIds().entrySet()) {
            List<String> projectIds = new ArrayList<>();
            for (Project project : entry.getValue()) {
                projectIds.add(duplicateHandler.getUniqueId(project));
            }
            duplicateArtifactIds.put(entry.getKey(), projectIds);
        }
        issues.put("duplicateArtifactIds", duplicateArtifactIds);
        
        // Duplicate GAVs
        Map<String, List<String>> duplicateGAVs = new HashMap<>();
        for (Map.Entry<String, List<Project>> entry : issuesReport.getDuplicateGAVs().entrySet()) {
            List<String> projectIds = new ArrayList<>();
            for (Project project : entry.getValue()) {
                projectIds.add(duplicateHandler.getUniqueId(project));
            }
            duplicateGAVs.put(entry.getKey(), projectIds);
        }
        issues.put("duplicateGAVs", duplicateGAVs);
        
        result.put("issues", issues);
        
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
            String nodeGroup = determineNodeGroup(project);
            if (nodeGroup == null || nodeGroup.isEmpty()) {
                nodeGroup = "default";
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
    
    private String determineNodeGroup(Project project) {
        logger.debug("Determining node group for project: {}", project.getFullName());
        logger.debug("  - ArtifactId: {}", project.getArtifactId());
        logger.debug("  - GroupId: {}", project.getGroupId());
        logger.debug("  - ProjectPath: {}", project.getProjectPath());
        
        // Priority 1: Use repository directory name if meaningful
        String repoName = getRepositoryName(project);
        if (repoName != null && !isGenericDirectoryName(repoName)) {
            logger.debug("  -> Using repository name as nodeGroup: {}", repoName);
            return repoName;
        }
        
        // Priority 2: Use artifact ID if it makes sense as a project name
        if (project.getArtifactId() != null && !project.getArtifactId().isEmpty()) {
            String artifactId = project.getArtifactId();
            
            // If artifact ID is meaningful (not just generic names), use it
            if (!isGenericArtifactId(artifactId)) {
                logger.debug("  -> Using artifact ID as nodeGroup: {}", artifactId);
                return artifactId;
            } else {
                logger.debug("  - Artifact ID '{}' is generic, checking combinations", artifactId);
                
                // Try combining with group ID for generic artifact IDs
                if (project.getGroupId() != null && !project.getGroupId().isEmpty()) {
                    String[] groupParts = project.getGroupId().split("\\.");
                    if (groupParts.length >= 2) {
                        // Use last two parts of group ID + artifact ID for generic names
                        String combined = groupParts[groupParts.length - 2] + "-" + 
                                        groupParts[groupParts.length - 1] + "-" + artifactId;
                        logger.debug("  -> Using combined group+artifact as nodeGroup: {}", combined);
                        return combined;
                    }
                }
            }
        }
        
        // Priority 3: Use group ID's last component(s)
        if (project.getGroupId() != null && !project.getGroupId().isEmpty()) {
            String[] groupParts = project.getGroupId().split("\\.");
            if (groupParts.length > 0) {
                String lastPart = groupParts[groupParts.length - 1];
                if (!lastPart.isEmpty() && !isGenericGroupPart(lastPart)) {
                    logger.debug("  -> Using group ID last part as nodeGroup: {}", lastPart);
                    return lastPart;
                } else if (groupParts.length >= 2) {
                    // Try using last two parts
                    String lastTwoParts = groupParts[groupParts.length - 2] + "-" + 
                                         groupParts[groupParts.length - 1];
                    logger.debug("  -> Using group ID last two parts as nodeGroup: {}", lastTwoParts);
                    return lastTwoParts;
                }
            }
        }
        
        // Priority 4: Use repository name even if generic (better than nothing)
        if (repoName != null) {
            logger.debug("  -> Fallback to repository name as nodeGroup: {}", repoName);
            return repoName;
        }
        
        // Priority 5: Fallback to artifact ID even if generic
        if (project.getArtifactId() != null && !project.getArtifactId().isEmpty()) {
            logger.debug("  -> Fallback to artifact ID as nodeGroup: {}", project.getArtifactId());
            return project.getArtifactId();
        }
        
        logger.debug("  -> Using 'unknown' as nodeGroup");
        return "unknown";
    }
    
    private String getRepositoryName(Project project) {
        if (project.getProjectPath() == null) {
            return null;
        }
        
        Path repoPath = project.getProjectPath();
        // Find the git repository root
        while (repoPath != null && !Files.exists(repoPath.resolve(".git"))) {
            repoPath = repoPath.getParent();
        }
        
        if (repoPath != null) {
            return repoPath.getFileName().toString();
        }
        return null;
    }
    
    private boolean isGenericArtifactId(String artifactId) {
        Set<String> genericNames = Set.of(
            "project", "app", "application", "service", "module", 
            "core", "common", "util", "utils", "lib", "library"
        );
        return genericNames.contains(artifactId.toLowerCase());
    }
    
    private boolean isGenericGroupPart(String groupPart) {
        Set<String> genericParts = Set.of(
            "example", "test", "demo", "sample", "project", 
            "app", "application", "com", "org", "net"
        );
        return genericParts.contains(groupPart.toLowerCase());
    }
    
    private boolean isGenericDirectoryName(String dirName) {
        Set<String> genericDirs = Set.of(
            "test", "demo", "sample", "examples", 
            "projects", "project", "repos", "repositories", "repository",
            "workspace", "workspaces", "src", "source", "sources"
        );
        
        // Also check for patterns like "demo-*", "sample-*"
        String lowerDirName = dirName.toLowerCase();
        if (lowerDirName.startsWith("demo-") || 
            lowerDirName.startsWith("sample-") ||
            lowerDirName.startsWith("example-")) {
            return true;
        }
        
        return genericDirs.contains(lowerDirName);
    }
    
    private void saveAnalysisResult(Map<String, Object> analysisResult) throws IOException {
        saveAnalysisResult(analysisResult, null);
    }
    
    private void saveAnalysisResult(Map<String, Object> analysisResult, String outputFile) throws IOException {
        File targetFile;
        
        if (outputFile != null) {
            // Use explicit output file path
            targetFile = new File(outputFile);
        } else {
            // Use configuration from system properties or defaults
            String outputDir = System.getProperty("analyzer.output.directory", "./frontend/public");
            String outputFilename = System.getProperty("analyzer.output.filename", "dependencies-analysis.json");
            
            // Create output directory if it doesn't exist
            File dir = new File(outputDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (created) {
                    logger.info("Created output directory: {}", dir.getAbsolutePath());
                }
            }
            
            targetFile = new File(dir, outputFilename);
            
            // Handle backup if enabled
            String createBackup = System.getProperty("analyzer.output.create-backup", "true");
            if ("true".equalsIgnoreCase(createBackup) && targetFile.exists()) {
                String backupName = outputFilename + ".backup." + System.currentTimeMillis();
                File backupFile = new File(dir, backupName);
                Files.copy(targetFile.toPath(), backupFile.toPath());
                logger.info("Created backup: {}", backupFile.getAbsolutePath());
            }
        }
        
        mapper.writeValue(targetFile, analysisResult);
        logger.info("Analysis result saved to: {}", targetFile.getAbsolutePath());
    }
    
    private void printSummary(Map<String, Object> analysisResult) {
        @SuppressWarnings("unchecked")
        Map<String, Object> stats = (Map<String, Object>) analysisResult.get("stats");
        
        System.out.println("\n=== Analysis Summary ===");
        System.out.println("Total projects found: " + stats.get("totalProjects"));
        System.out.println("Total in-house dependencies: " + stats.get("totalDependencies"));
        System.out.println("\nTo visualize the results:");
        System.out.println("1. cd frontend");
        System.out.println("2. npm start");
        System.out.println("3. Open http://localhost:3030 (analysis will be loaded automatically)");
    }
}