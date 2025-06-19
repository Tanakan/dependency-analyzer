package com.example.dependencies.analyzer.analyzer;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class InHouseProjectDetector {
    private static final Logger logger = LoggerFactory.getLogger(InHouseProjectDetector.class);
    
    private final Map<String, Project> projectMap;
    private final Map<String, List<Project>> duplicateProjectsMap;
    private final List<Project> allProjects;
    private final Set<String> inHouseGroupIds;

    public InHouseProjectDetector(List<Project> allProjects) {
        this.allProjects = new ArrayList<>(allProjects);
        this.projectMap = new HashMap<>();
        this.duplicateProjectsMap = new HashMap<>();
        this.inHouseGroupIds = new HashSet<>();
        
        // Build project map and collect in-house group IDs
        // Store all duplicate projects separately
        for (Project project : allProjects) {
            String key = project.getGroupId() + ":" + project.getArtifactId();
            
            // Add to duplicate projects map
            duplicateProjectsMap.computeIfAbsent(key, k -> new ArrayList<>()).add(project);
            
            if (projectMap.containsKey(key)) {
                logger.warn("Duplicate project found: {} in {} and {}",
                    key, projectMap.get(key).getProjectPath(), project.getProjectPath());
            }
            projectMap.put(key, project);
            inHouseGroupIds.add(project.getGroupId());
        }
        
        logger.info("Detected {} in-house projects with group IDs: {}", 
                   allProjects.size(), inHouseGroupIds);
    }

    public boolean isInHouseProject(Dependency dependency) {
        // Check if dependency matches any known in-house project
        String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
        return projectMap.containsKey(key) || inHouseGroupIds.contains(dependency.getGroupId());
    }

    public List<Dependency> filterInHouseDependencies(Project project) {
        return project.getDependencies().stream()
            .filter(this::isInHouseProject)
            .collect(Collectors.toList());
    }

    public Map<Project, List<Dependency>> buildInHouseDependencyMap() {
        Map<Project, List<Dependency>> dependencyMap = new LinkedHashMap<>();
        
        // Use allProjects to preserve duplicates
        for (Project project : allProjects) {
            List<Dependency> inHouseDeps = filterInHouseDependencies(project);
            if (!inHouseDeps.isEmpty() || allProjects.size() < 50) {
                // Include projects with dependencies or all projects if small set
                dependencyMap.put(project, inHouseDeps);
                logger.debug("Project {} has {} in-house dependencies", 
                           project.getFullName(), inHouseDeps.size());
            }
        }
        
        return dependencyMap;
    }

    public Project findProject(Dependency dependency) {
        String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
        return projectMap.get(key);
    }
    
    public List<Project> findAllProjects(Dependency dependency) {
        String key = dependency.getGroupId() + ":" + dependency.getArtifactId();
        return duplicateProjectsMap.getOrDefault(key, Collections.emptyList());
    }

    public Collection<Project> getAllProjects() {
        return allProjects;
    }
}