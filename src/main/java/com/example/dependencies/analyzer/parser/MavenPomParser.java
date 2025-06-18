package com.example.dependencies.analyzer.parser;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.nio.file.Path;
import java.util.Properties;

public class MavenPomParser {
    private static final Logger logger = LoggerFactory.getLogger(MavenPomParser.class);

    public Project parse(Path pomPath) {
        try (FileReader reader = new FileReader(pomPath.toFile())) {
            MavenXpp3Reader mavenReader = new MavenXpp3Reader();
            Model model = mavenReader.read(reader);

            // Handle parent POM values
            String groupId = model.getGroupId();
            String version = model.getVersion();
            
            if (groupId == null && model.getParent() != null) {
                groupId = model.getParent().getGroupId();
            }
            if (version == null && model.getParent() != null) {
                version = model.getParent().getVersion();
            }

            Project project = new Project(
                groupId,
                model.getArtifactId(),
                version,
                pomPath.getParent(),
                ProjectType.MAVEN
            );
            
            // Set packaging type (default is jar if not specified)
            String packaging = model.getPackaging();
            if (packaging != null && !packaging.isEmpty()) {
                project.setPackaging(packaging);
            } else {
                project.setPackaging("jar"); // Default to jar
            }

            // Parse dependencies
            if (model.getDependencies() != null) {
                for (org.apache.maven.model.Dependency dep : model.getDependencies()) {
                    String depVersion = resolveVersion(dep.getVersion(), model);
                    Dependency dependency = new Dependency(
                        dep.getGroupId(),
                        dep.getArtifactId(),
                        depVersion,
                        dep.getScope() != null ? dep.getScope() : "compile"
                    );
                    project.addDependency(dependency);
                }
            }

            logger.debug("Parsed Maven project: {}", project.getFullName());
            return project;

        } catch (Exception e) {
            logger.error("Error parsing POM file: " + pomPath, e);
            throw new RuntimeException("Failed to parse POM file: " + pomPath, e);
        }
    }

    private String resolveVersion(String version, Model model) {
        if (version == null) {
            return "unknown";
        }
        
        // Handle property placeholders like ${project.version}
        if (version.startsWith("${") && version.endsWith("}")) {
            String propertyName = version.substring(2, version.length() - 1);
            
            // Check common properties
            if ("project.version".equals(propertyName) || "version".equals(propertyName)) {
                return model.getVersion() != null ? model.getVersion() : 
                       (model.getParent() != null ? model.getParent().getVersion() : "unknown");
            }
            
            // Check properties section
            Properties properties = model.getProperties();
            if (properties != null && properties.containsKey(propertyName)) {
                return properties.getProperty(propertyName);
            }
            
            return version; // Return as-is if can't resolve
        }
        
        return version;
    }
}