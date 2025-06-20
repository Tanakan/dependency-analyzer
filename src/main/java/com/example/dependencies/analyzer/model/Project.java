package com.example.dependencies.analyzer.model;

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
            // Get absolute path and convert to string
            String fullPath = projectPath.toAbsolutePath().toString().replace('\\', '/');
            
            // Find test-projects in the path
            int testProjectsIndex = fullPath.indexOf("test-projects/");
            if (testProjectsIndex >= 0) {
                // Extract the part after test-projects/
                String afterTestProjects = fullPath.substring(testProjectsIndex + "test-projects/".length());
                
                // Extract repository name (first directory after test-projects/)
                int nextSlash = afterTestProjects.indexOf('/');
                if (nextSlash > 0) {
                    return afterTestProjects.substring(0, nextSlash);
                } else if (!afterTestProjects.isEmpty()) {
                    return afterTestProjects;
                }
            }
            
            // If path doesn't contain test-projects, try to extract last two directories
            String[] parts = fullPath.split("/");
            if (parts.length >= 2 && parts[parts.length - 2].equals("test-projects")) {
                return parts[parts.length - 1];
            }
            
            return "unknown";
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