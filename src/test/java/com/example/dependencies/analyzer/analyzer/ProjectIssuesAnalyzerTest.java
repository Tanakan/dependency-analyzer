package com.example.dependencies.analyzer.analyzer;

import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.ProjectType;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ProjectIssuesAnalyzerTest {
    
    @Test
    void testCircularReferenceDetection() {
        // Create projects with circular dependency
        Project projectA = new Project("com.example", "service-a", "1.0.0", Paths.get("service-a"), ProjectType.MAVEN);
        Project projectB = new Project("com.example", "service-b", "1.0.0", Paths.get("service-b"), ProjectType.MAVEN);
        Project projectC = new Project("com.example", "service-c", "1.0.0", Paths.get("service-c"), ProjectType.MAVEN);
        
        projectA.addDependency(new Dependency("com.example", "service-b", "1.0.0", "compile"));
        projectB.addDependency(new Dependency("com.example", "service-c", "1.0.0", "compile"));
        projectC.addDependency(new Dependency("com.example", "service-a", "1.0.0", "compile"));
        
        List<Project> projects = Arrays.asList(projectA, projectB, projectC);
        Map<Project, List<Dependency>> dependencyMap = new HashMap<>();
        
        for (Project p : projects) {
            dependencyMap.put(p, new ArrayList<>(p.getDependencies()));
        }
        
        ProjectIssuesAnalyzer analyzer = new ProjectIssuesAnalyzer(projects, dependencyMap);
        List<List<String>> cycles = analyzer.detectCircularReferences();
        
        assertEquals(1, cycles.size(), "Should detect one circular reference");
        assertEquals(4, cycles.get(0).size(), "Cycle should contain 4 elements (3 projects + repeat)");
    }
    
    @Test
    void testUnreferencedProjectsDetection() {
        Project app = new Project("com.example", "app", "1.0.0", Paths.get("app"), ProjectType.MAVEN);
        Project lib1 = new Project("com.example", "lib1", "1.0.0", Paths.get("lib1"), ProjectType.MAVEN);
        Project lib2 = new Project("com.example", "lib2", "1.0.0", Paths.get("lib2"), ProjectType.MAVEN);
        Project standalone = new Project("com.example", "standalone", "1.0.0", Paths.get("standalone"), ProjectType.MAVEN);
        
        app.addDependency(new Dependency("com.example", "lib1", "1.0.0", "compile"));
        lib1.addDependency(new Dependency("com.example", "lib2", "1.0.0", "compile"));
        
        List<Project> projects = Arrays.asList(app, lib1, lib2, standalone);
        Map<Project, List<Dependency>> dependencyMap = new HashMap<>();
        
        for (Project p : projects) {
            dependencyMap.put(p, new ArrayList<>(p.getDependencies()));
        }
        
        ProjectIssuesAnalyzer analyzer = new ProjectIssuesAnalyzer(projects, dependencyMap);
        List<Project> unreferenced = analyzer.detectUnreferencedProjects();
        
        assertEquals(2, unreferenced.size(), "Should find 2 unreferenced projects");
        assertTrue(unreferenced.contains(app), "App should be unreferenced");
        assertTrue(unreferenced.contains(standalone), "Standalone should be unreferenced");
    }
    
    @Test
    void testDuplicateArtifactIdDetection() {
        Project moduleA1 = new Project("com.example.group1", "module-a", "1.0.0", Paths.get("group1/module-a"), ProjectType.MAVEN);
        Project moduleA2 = new Project("com.example.group2", "module-a", "1.0.0", Paths.get("group2/module-a"), ProjectType.MAVEN);
        Project moduleB1 = new Project("com.example.group1", "module-b", "1.0.0", Paths.get("group1/module-b"), ProjectType.MAVEN);
        Project unique = new Project("com.example", "unique", "1.0.0", Paths.get("unique"), ProjectType.MAVEN);
        
        List<Project> projects = Arrays.asList(moduleA1, moduleA2, moduleB1, unique);
        Map<Project, List<Dependency>> dependencyMap = new HashMap<>();
        
        ProjectIssuesAnalyzer analyzer = new ProjectIssuesAnalyzer(projects, dependencyMap);
        Map<String, List<Project>> duplicates = analyzer.detectDuplicateArtifactIds();
        
        assertEquals(1, duplicates.size(), "Should find 1 duplicate artifact ID");
        assertTrue(duplicates.containsKey("module-a"), "Should detect module-a as duplicate");
        assertEquals(2, duplicates.get("module-a").size(), "module-a should have 2 instances");
    }
    
    @Test
    void testCompleteAnalysis() {
        // Create a complex scenario
        Project app = new Project("com.example", "app", "1.0.0", Paths.get("app"), ProjectType.MAVEN);
        Project service1 = new Project("com.example", "service1", "1.0.0", Paths.get("service1"), ProjectType.MAVEN);
        Project service2 = new Project("com.example", "service2", "1.0.0", Paths.get("service2"), ProjectType.MAVEN);
        Project common1 = new Project("com.example.group1", "common", "1.0.0", Paths.get("group1/common"), ProjectType.MAVEN);
        Project common2 = new Project("com.example.group2", "common", "1.0.0", Paths.get("group2/common"), ProjectType.MAVEN);
        
        // Create circular dependency
        service1.addDependency(new Dependency("com.example", "service2", "1.0.0", "compile"));
        service2.addDependency(new Dependency("com.example", "service1", "1.0.0", "compile"));
        
        // App depends on service1
        app.addDependency(new Dependency("com.example", "service1", "1.0.0", "compile"));
        
        List<Project> projects = Arrays.asList(app, service1, service2, common1, common2);
        Map<Project, List<Dependency>> dependencyMap = new HashMap<>();
        
        for (Project p : projects) {
            dependencyMap.put(p, new ArrayList<>(p.getDependencies()));
        }
        
        ProjectIssuesAnalyzer analyzer = new ProjectIssuesAnalyzer(projects, dependencyMap);
        ProjectIssuesAnalyzer.IssuesReport report = analyzer.analyzeAll();
        
        assertEquals(1, report.getCircularReferences().size(), "Should find 1 circular reference");
        assertEquals(3, report.getUnreferencedProjects().size(), "Should find 3 unreferenced projects (app, common1, common2)");
        assertEquals(1, report.getDuplicateArtifactIds().size(), "Should find 1 duplicate artifact ID");
    }
}