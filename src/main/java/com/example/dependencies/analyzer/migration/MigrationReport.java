package com.example.dependencies.analyzer.migration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MigrationReport {
    private Set<String> unusedProjects;
    private List<List<String>> circularDependencies;
    private Map<String, Double> projectCohesionScores;
    private String analysisDate;


    public Set<String> getUnusedProjects() {
        return unusedProjects;
    }

    public void setUnusedProjects(Set<String> unusedProjects) {
        this.unusedProjects = unusedProjects;
    }

    public List<List<String>> getCircularDependencies() {
        return circularDependencies;
    }

    public void setCircularDependencies(List<List<String>> circularDependencies) {
        this.circularDependencies = circularDependencies;
    }

    public Map<String, Double> getProjectCohesionScores() {
        return projectCohesionScores;
    }

    public void setProjectCohesionScores(Map<String, Double> projectCohesionScores) {
        this.projectCohesionScores = projectCohesionScores;
    }


    public String getAnalysisDate() {
        return analysisDate;
    }

    public void setAnalysisDate(String analysisDate) {
        this.analysisDate = analysisDate;
    }
}