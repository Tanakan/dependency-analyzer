package com.example.dependencies.analyzer.analyzer;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ProjectIssuesAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectIssuesAnalyzer.class);
    
    private final List<Project> allProjects;
    private final Map<String, Project> projectMap;
    private final Map<Project, List<Dependency>> dependencyMap;
    
    public ProjectIssuesAnalyzer(List<Project> projects, Map<Project, List<Dependency>> dependencyMap) {
        this.allProjects = new ArrayList<>(projects);
        this.projectMap = new HashMap<>();
        this.dependencyMap = dependencyMap;
        
        // Build project map
        for (Project project : projects) {
            String key = project.getGroupId() + ":" + project.getArtifactId();
            projectMap.put(key, project);
        }
    }
    
    /**
     * 循環参照を検出
     */
    public List<List<String>> detectCircularReferences() {
        List<List<String>> cycles = new ArrayList<>();
        Set<Project> visited = new HashSet<>();
        
        for (Project project : allProjects) {
            if (!visited.contains(project)) {
                Set<Project> currentPath = new LinkedHashSet<>();
                detectCycles(project, visited, currentPath, cycles);
            }
        }
        
        return cycles;
    }
    
    private void detectCycles(Project project, Set<Project> visited, Set<Project> currentPath, List<List<String>> cycles) {
        if (currentPath.contains(project)) {
            // Found a cycle
            List<String> cycle = new ArrayList<>();
            boolean inCycle = false;
            for (Project p : currentPath) {
                if (p.equals(project)) {
                    inCycle = true;
                }
                if (inCycle) {
                    cycle.add(p.getFullName());
                }
            }
            cycle.add(project.getFullName());
            cycles.add(cycle);
            return;
        }
        
        if (visited.contains(project)) {
            return;
        }
        
        currentPath.add(project);
        
        List<Dependency> deps = dependencyMap.get(project);
        if (deps != null) {
            for (Dependency dep : deps) {
                String depKey = dep.getGroupId() + ":" + dep.getArtifactId();
                Project depProject = projectMap.get(depKey);
                if (depProject != null) {
                    detectCycles(depProject, visited, currentPath, cycles);
                }
            }
        }
        
        currentPath.remove(project);
        visited.add(project);
    }
    
    /**
     * 参照されていないプロジェクトを検出
     */
    public List<Project> detectUnreferencedProjects() {
        Set<String> referencedProjects = new HashSet<>();
        
        // Collect all referenced projects
        for (List<Dependency> deps : dependencyMap.values()) {
            for (Dependency dep : deps) {
                String key = dep.getGroupId() + ":" + dep.getArtifactId();
                referencedProjects.add(key);
            }
        }
        
        // Find projects that are not referenced
        List<Project> unreferenced = new ArrayList<>();
        for (Project project : allProjects) {
            String key = project.getGroupId() + ":" + project.getArtifactId();
            if (!referencedProjects.contains(key)) {
                unreferenced.add(project);
            }
        }
        
        return unreferenced;
    }
    
    /**
     * 同じartifactIdを持つプロジェクトを検出
     */
    public Map<String, List<Project>> detectDuplicateArtifactIds() {
        Map<String, List<Project>> artifactIdGroups = new HashMap<>();
        
        // Group projects by artifactId
        for (Project project : allProjects) {
            artifactIdGroups
                .computeIfAbsent(project.getArtifactId(), k -> new ArrayList<>())
                .add(project);
        }
        
        // Filter to keep only duplicates
        return artifactIdGroups.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue,
                (e1, e2) -> e1,
                LinkedHashMap::new
            ));
    }
    
    /**
     * 全ての問題を分析
     */
    public IssuesReport analyzeAll() {
        IssuesReport report = new IssuesReport();
        
        report.setCircularReferences(detectCircularReferences());
        report.setUnreferencedProjects(detectUnreferencedProjects());
        report.setDuplicateArtifactIds(detectDuplicateArtifactIds());
        
        logger.info("Found {} circular references", report.getCircularReferences().size());
        logger.info("Found {} unreferenced projects", report.getUnreferencedProjects().size());
        logger.info("Found {} duplicate artifact IDs", report.getDuplicateArtifactIds().size());
        
        return report;
    }
    
    public static class IssuesReport {
        private List<List<String>> circularReferences = new ArrayList<>();
        private List<Project> unreferencedProjects = new ArrayList<>();
        private Map<String, List<Project>> duplicateArtifactIds = new LinkedHashMap<>();
        
        public List<List<String>> getCircularReferences() {
            return circularReferences;
        }
        
        public void setCircularReferences(List<List<String>> circularReferences) {
            this.circularReferences = circularReferences;
        }
        
        public List<Project> getUnreferencedProjects() {
            return unreferencedProjects;
        }
        
        public void setUnreferencedProjects(List<Project> unreferencedProjects) {
            this.unreferencedProjects = unreferencedProjects;
        }
        
        public Map<String, List<Project>> getDuplicateArtifactIds() {
            return duplicateArtifactIds;
        }
        
        public void setDuplicateArtifactIds(Map<String, List<Project>> duplicateArtifactIds) {
            this.duplicateArtifactIds = duplicateArtifactIds;
        }
    }
}