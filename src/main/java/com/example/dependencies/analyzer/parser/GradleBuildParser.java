package com.example.dependencies.analyzer.parser;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GradleBuildParser {
    private static final Logger logger = LoggerFactory.getLogger(GradleBuildParser.class);
    
    // Patterns for extracting project information
    private static final Pattern GROUP_PATTERN = Pattern.compile("group\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern VERSION_PATTERN = Pattern.compile("version\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern ARTIFACT_PATTERN = Pattern.compile("rootProject\\.name\\s*=\\s*['\"]([^'\"]+)['\"]");
    
    // Patterns for dependencies
    private static final Pattern DEPENDENCY_PATTERN = Pattern.compile(
        "(?:implementation|api|compile|compileOnly|runtimeOnly|testImplementation)\\s*" +
        "(?:\\()?['\"]([^'\"]+)['\"](?:\\))?"
    );
    
    // Pattern for parsing dependency notation (group:artifact:version)
    private static final Pattern DEPENDENCY_NOTATION = Pattern.compile(
        "([^:]+):([^:]+)(?::([^:]+))?"
    );

    public Project parse(Path buildFilePath) {
        try {
            String content = Files.readString(buildFilePath);
            
            // Try to get artifact name from settings.gradle[.kts]
            String artifactId = findArtifactId(buildFilePath.getParent());
            
            // Extract group and version from build file
            String groupId = extractPattern(content, GROUP_PATTERN, null);
            String version = extractPattern(content, VERSION_PATTERN, null);
            
            // If group/version not found, try parent build.gradle files
            if (groupId == null || version == null) {
                Path currentPath = buildFilePath.getParent();
                
                // Search up to 3 levels up for parent build files
                for (int i = 0; i < 3 && (groupId == null || version == null); i++) {
                    currentPath = currentPath.getParent();
                    if (currentPath == null) break;
                    
                    Path parentBuildFile = currentPath.resolve("build.gradle");
                    if (!Files.exists(parentBuildFile)) {
                        parentBuildFile = currentPath.resolve("build.gradle.kts");
                    }
                    
                    if (Files.exists(parentBuildFile)) {
                        try {
                            String parentContent = Files.readString(parentBuildFile);
                            if (groupId == null) {
                                String foundGroup = extractPattern(parentContent, GROUP_PATTERN, null);
                                if (foundGroup != null) {
                                    groupId = foundGroup;
                                    logger.debug("Found group {} in parent: {}", groupId, parentBuildFile);
                                }
                            }
                            if (version == null) {
                                String foundVersion = extractPattern(parentContent, VERSION_PATTERN, null);
                                if (foundVersion != null) {
                                    version = foundVersion;
                                    logger.debug("Found version {} in parent: {}", version, parentBuildFile);
                                }
                            }
                        } catch (IOException e) {
                            logger.debug("Could not read parent build file: " + parentBuildFile);
                        }
                    }
                }
            }
            
            // Final defaults
            if (groupId == null) groupId = "unknown.group";
            if (version == null) version = "unknown";
            
            // If artifact ID not found in settings, use directory name
            if (artifactId == null) {
                artifactId = buildFilePath.getParent().getFileName().toString();
            }

            Project project = new Project(
                groupId,
                artifactId,
                version,
                buildFilePath.getParent(),
                ProjectType.GRADLE
            );
            
            // Detect packaging type from plugins
            if (content.contains("war") || content.contains("'war'") || 
                content.contains("\"war\"") || content.contains("id 'war'") ||
                content.contains("id \"war\"") || content.contains("apply plugin: 'war'") ||
                content.contains("apply plugin: \"war\"")) {
                project.setPackaging("war");
            }
            // Check for Spring Boot plugin which can create executable JARs
            else if (content.contains("org.springframework.boot") || 
                     content.contains("bootJar") || content.contains("bootWar")) {
                // If bootWar task is used, it's a WAR
                if (content.contains("bootWar")) {
                    project.setPackaging("war");
                } else {
                    project.setPackaging("jar");
                }
            } else {
                // Default to jar if no specific packaging is detected
                project.setPackaging("jar");
            }

            // Parse dependencies
            parseDependencies(content, project);

            logger.debug("Parsed Gradle project: {}", project.getFullName());
            return project;

        } catch (IOException e) {
            logger.error("Error parsing Gradle build file: " + buildFilePath, e);
            throw new RuntimeException("Failed to parse Gradle build file: " + buildFilePath, e);
        }
    }

    private String findArtifactId(Path projectDir) {
        // Check settings.gradle or settings.gradle.kts
        Path settingsGradle = projectDir.resolve("settings.gradle");
        Path settingsGradleKts = projectDir.resolve("settings.gradle.kts");
        
        Path settingsFile = Files.exists(settingsGradle) ? settingsGradle : 
                           (Files.exists(settingsGradleKts) ? settingsGradleKts : null);
        
        if (settingsFile != null) {
            try {
                String content = Files.readString(settingsFile);
                return extractPattern(content, ARTIFACT_PATTERN, null);
            } catch (IOException e) {
                logger.warn("Could not read settings file: " + settingsFile);
            }
        }
        
        return null;
    }

    private String extractPattern(String content, Pattern pattern, String defaultValue) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return defaultValue;
    }

    private void parseDependencies(String content, Project project) {
        Matcher matcher = DEPENDENCY_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String dependencyNotation = matcher.group(1);
            
            // Parse dependency notation (group:artifact:version)
            Matcher notationMatcher = DEPENDENCY_NOTATION.matcher(dependencyNotation);
            if (notationMatcher.matches()) {
                String groupId = notationMatcher.group(1);
                String artifactId = notationMatcher.group(2);
                String version = notationMatcher.group(3) != null ? notationMatcher.group(3) : "unknown";
                
                // Skip Gradle/Kotlin specific dependencies
                if (isGradleInternalDependency(groupId)) {
                    continue;
                }
                
                Dependency dependency = new Dependency(
                    groupId,
                    artifactId,
                    version,
                    "compile" // Gradle doesn't expose scope in the same way
                );
                project.addDependency(dependency);
            }
        }
    }

    private boolean isGradleInternalDependency(String groupId) {
        return groupId.startsWith("org.gradle") || 
               groupId.startsWith("org.jetbrains.kotlin") ||
               groupId.equals("kotlin");
    }
}