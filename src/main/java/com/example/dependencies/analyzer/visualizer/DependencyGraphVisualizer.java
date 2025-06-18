package com.example.dependencies.analyzer.visualizer;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class DependencyGraphVisualizer {
    
    public String visualize(Map<Project, List<Dependency>> dependencyMap) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        
        pw.println("\n=== In-House Project Dependencies ===\n");
        
        // Group projects by repository (parent directory)
        Map<String, List<Project>> projectsByRepo = groupProjectsByRepository(dependencyMap.keySet());
        
        for (Map.Entry<String, List<Project>> repoEntry : projectsByRepo.entrySet()) {
            pw.println("Repository: " + repoEntry.getKey());
            pw.println("─".repeat(50));
            
            for (Project project : repoEntry.getValue()) {
                printProjectDependencies(pw, project, dependencyMap.get(project));
            }
            pw.println();
        }
        
        // Print dependency statistics
        printStatistics(pw, dependencyMap);
        
        // Print dependency tree
        pw.println("\n=== Dependency Tree ===\n");
        printDependencyTree(pw, dependencyMap);
        
        return sw.toString();
    }
    
    private Map<String, List<Project>> groupProjectsByRepository(Set<Project> projects) {
        Map<String, List<Project>> grouped = new TreeMap<>();
        
        for (Project project : projects) {
            String repoName = project.getProjectPath().getParent().getFileName().toString();
            grouped.computeIfAbsent(repoName, k -> new ArrayList<>()).add(project);
        }
        
        return grouped;
    }
    
    private void printProjectDependencies(PrintWriter pw, Project project, List<Dependency> dependencies) {
        pw.printf("  %s (%s)\n", project.getFullName(), project.getType().getDisplayName());
        
        if (dependencies == null || dependencies.isEmpty()) {
            pw.println("    └─ No in-house dependencies");
        } else {
            for (int i = 0; i < dependencies.size(); i++) {
                Dependency dep = dependencies.get(i);
                boolean isLast = (i == dependencies.size() - 1);
                String prefix = isLast ? "└─" : "├─";
                pw.printf("    %s %s\n", prefix, dep.getFullName());
            }
        }
    }
    
    private void printStatistics(PrintWriter pw, Map<Project, List<Dependency>> dependencyMap) {
        pw.println("\n=== Statistics ===\n");
        
        int totalProjects = dependencyMap.size();
        int totalDependencies = dependencyMap.values().stream()
            .mapToInt(List::size)
            .sum();
        
        // Find most dependent projects
        List<Map.Entry<Project, List<Dependency>>> sortedByDeps = new ArrayList<>(dependencyMap.entrySet());
        sortedByDeps.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));
        
        pw.printf("Total in-house projects: %d\n", totalProjects);
        pw.printf("Total in-house dependencies: %d\n", totalDependencies);
        pw.println("\nMost dependent projects:");
        
        sortedByDeps.stream()
            .limit(5)
            .forEach(entry -> {
                pw.printf("  - %s: %d dependencies\n", 
                    entry.getKey().getFullName(), 
                    entry.getValue().size());
            });
    }
    
    private void printDependencyTree(PrintWriter pw, Map<Project, List<Dependency>> dependencyMap) {
        Set<Project> roots = findRootProjects(dependencyMap);
        
        pw.println("Root projects (no in-house dependencies):");
        for (Project root : roots) {
            pw.println("  - " + root.getFullName());
        }
        
        pw.println("\nDependency hierarchy:");
        Set<Project> visited = new HashSet<>();
        
        for (Project root : roots) {
            printProjectTree(pw, root, dependencyMap, "", visited);
        }
    }
    
    private Set<Project> findRootProjects(Map<Project, List<Dependency>> dependencyMap) {
        Set<Project> allProjects = new HashSet<>(dependencyMap.keySet());
        Set<Project> hasNoDependencies = new HashSet<>();
        
        for (Map.Entry<Project, List<Dependency>> entry : dependencyMap.entrySet()) {
            if (entry.getValue().isEmpty()) {
                hasNoDependencies.add(entry.getKey());
            }
        }
        
        // Also include projects that are not in the dependency map (no dependencies at all)
        return hasNoDependencies.isEmpty() ? allProjects : hasNoDependencies;
    }
    
    private void printProjectTree(PrintWriter pw, Project project, 
                                  Map<Project, List<Dependency>> dependencyMap,
                                  String indent, Set<Project> visited) {
        if (visited.contains(project)) {
            pw.println(indent + project.getArtifactId() + " [circular reference]");
            return;
        }
        
        visited.add(project);
        pw.println(indent + project.getArtifactId() + " (" + project.getVersion() + ")");
        
        // Find projects that depend on this project
        List<Project> dependents = findDependents(project, dependencyMap);
        
        for (int i = 0; i < dependents.size(); i++) {
            boolean isLast = (i == dependents.size() - 1);
            String newIndent = indent + (isLast ? "  └─ " : "  ├─ ");
            String continueIndent = indent + (isLast ? "     " : "  │  ");
            
            printProjectTree(pw, dependents.get(i), dependencyMap, continueIndent, visited);
        }
        
        visited.remove(project);
    }
    
    private List<Project> findDependents(Project project, Map<Project, List<Dependency>> dependencyMap) {
        List<Project> dependents = new ArrayList<>();
        
        for (Map.Entry<Project, List<Dependency>> entry : dependencyMap.entrySet()) {
            for (Dependency dep : entry.getValue()) {
                if (dep.getGroupId().equals(project.getGroupId()) && 
                    dep.getArtifactId().equals(project.getArtifactId())) {
                    dependents.add(entry.getKey());
                    break;
                }
            }
        }
        
        return dependents;
    }
}