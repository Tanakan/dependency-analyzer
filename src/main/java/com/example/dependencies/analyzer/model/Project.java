package com.example.dependencies.analyzer.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Project {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final Path projectPath;
    private final ProjectType type;
    private String packaging;
    private final List<Dependency> dependencies;

    public Project(String groupId, String artifactId, String version, Path projectPath, ProjectType type) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.projectPath = projectPath;
        this.type = type;
        this.dependencies = new ArrayList<>();
    }

    public void addDependency(Dependency dependency) {
        this.dependencies.add(dependency);
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public Path getProjectPath() {
        return projectPath;
    }

    public ProjectType getType() {
        return type;
    }

    public List<Dependency> getDependencies() {
        return new ArrayList<>(dependencies);
    }
    
    public String getPackaging() {
        return packaging;
    }
    
    public void setPackaging(String packaging) {
        this.packaging = packaging;
    }

    public String getFullName() {
        return groupId + ":" + artifactId + ":" + version;
    }
    
    public String getRepository() {
        if (projectPath == null) return "unknown";
        
        try {
            // Find the git repository root
            Path currentPath = projectPath;
            while (currentPath != null) {
                if (Files.exists(currentPath.resolve(".git"))) {
                    // Return the repository directory name
                    return currentPath.getFileName().toString();
                }
                currentPath = currentPath.getParent();
            }
            
            // If no git repository found, use the project directory name
            return projectPath.getFileName().toString();
        } catch (Exception e) {
            return "unknown";
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(groupId, project.groupId) && 
               Objects.equals(artifactId, project.artifactId) && 
               Objects.equals(version, project.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, artifactId, version);
    }

    @Override
    public String toString() {
        return "Project{" +
                "fullName='" + getFullName() + '\'' +
                ", type=" + type +
                ", path=" + projectPath +
                '}';
    }
}