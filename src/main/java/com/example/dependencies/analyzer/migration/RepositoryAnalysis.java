package com.example.dependencies.analyzer.migration;

import java.util.Set;

public class RepositoryAnalysis {
    private String repositoryName;
    private int projectCount;
    private Set<String> projects;
    private double cohesionScore;
    private int internalDependencies;
    private int externalDependencies;
    private String cohesionLevel; // HIGH, MEDIUM, LOW
    
    public String getRepositoryName() {
        return repositoryName;
    }
    
    public void setRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
    }
    
    public int getProjectCount() {
        return projectCount;
    }
    
    public void setProjectCount(int projectCount) {
        this.projectCount = projectCount;
    }
    
    public Set<String> getProjects() {
        return projects;
    }
    
    public void setProjects(Set<String> projects) {
        this.projects = projects;
    }
    
    public double getCohesionScore() {
        return cohesionScore;
    }
    
    public void setCohesionScore(double cohesionScore) {
        this.cohesionScore = cohesionScore;
    }
    
    public int getInternalDependencies() {
        return internalDependencies;
    }
    
    public void setInternalDependencies(int internalDependencies) {
        this.internalDependencies = internalDependencies;
    }
    
    public int getExternalDependencies() {
        return externalDependencies;
    }
    
    public void setExternalDependencies(int externalDependencies) {
        this.externalDependencies = externalDependencies;
    }
    
    public String getCohesionLevel() {
        if (cohesionScore > 0.7) return "HIGH";
        if (cohesionScore > 0.4) return "MEDIUM";
        return "LOW";
    }
    
    public void setCohesionLevel(String cohesionLevel) {
        this.cohesionLevel = cohesionLevel;
    }
}