package com.example.dependencies.analyzer.controller;

import com.example.dependencies.analyzer.DependencyAnalyzer;
import com.example.dependencies.analyzer.migration.*;
import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/migration")
public class MigrationController {
    private static final Logger logger = LoggerFactory.getLogger(MigrationController.class);
    
    @GetMapping("/repository-cohesion")
    public Map<String, Object> getRepositoryCohesion() {
        try {
            Path rootPath = Paths.get("test-projects");
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            List<Project> allProjects = new ArrayList<>();
            List<Path> gitRepositories = analyzer.findGitRepositories(rootPath);
            for (Path repo : gitRepositories) {
                allProjects.addAll(analyzer.analyzeRepository(repo));
            }
            
            MigrationAnalyzer migrationAnalyzer = new MigrationAnalyzer(allProjects);
            
            // リポジトリごとに詳細な凝集度分析
            List<RepositoryAnalysis> analyses = new ArrayList<>();
            Map<String, Set<String>> projectsByRepo = groupProjectsByRepository(allProjects);
            
            for (Map.Entry<String, Set<String>> entry : projectsByRepo.entrySet()) {
                String repoName = entry.getKey();
                Set<String> repoProjects = entry.getValue();
                
                RepositoryAnalysis analysis = new RepositoryAnalysis();
                analysis.setRepositoryName(repoName);
                analysis.setProjectCount(repoProjects.size());
                analysis.setProjects(repoProjects);
                
                if (repoProjects.size() > 1) {
                    // 複数プロジェクトの場合のみ凝集度を計算
                    double cohesion = migrationAnalyzer.calculateCohesion(repoProjects);
                    analysis.setCohesionScore(cohesion);
                    
                    // 内部・外部依存関係のカウント
                    int[] counts = countDependencies(allProjects, repoProjects);
                    analysis.setInternalDependencies(counts[0]);
                    analysis.setExternalDependencies(counts[1]);
                } else {
                    // 単一プロジェクトの場合は凝集度1.0
                    analysis.setCohesionScore(1.0);
                    analysis.setInternalDependencies(0);
                    analysis.setExternalDependencies(countSingleProjectDependencies(allProjects, repoProjects.iterator().next()));
                }
                
                analyses.add(analysis);
            }
            
            // 結果をソート（複数プロジェクトかつ低凝集度を優先）
            analyses.sort((a, b) -> {
                if (a.getProjectCount() > 1 && b.getProjectCount() == 1) return -1;
                if (a.getProjectCount() == 1 && b.getProjectCount() > 1) return 1;
                return Double.compare(a.getCohesionScore(), b.getCohesionScore());
            });
            
            Map<String, Object> result = new HashMap<>();
            result.put("analyses", analyses);
            result.put("totalRepositories", analyses.size());
            result.put("multiProjectRepositories", analyses.stream().filter(a -> a.getProjectCount() > 1).count());
            
            return result;
        } catch (Exception e) {
            logger.error("Error analyzing repository cohesion", e);
            throw new RuntimeException("Repository cohesion analysis failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/analyze")
    public MigrationReport analyzeMigration(@RequestParam(required = false) String repository) {
        try {
            // Run dependency analysis
            Path rootPath = Paths.get("test-projects");
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            List<Project> allProjects = new ArrayList<>();
            List<Path> gitRepositories = analyzer.findGitRepositories(rootPath);
            for (Path repo : gitRepositories) {
                allProjects.addAll(analyzer.analyzeRepository(repo));
            }
            
            // Create migration analyzer
            MigrationAnalyzer migrationAnalyzer = new MigrationAnalyzer(allProjects);
            
            // Build migration report
            MigrationReport report = new MigrationReport();
            report.setAnalysisDate(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
            
            // Find unused projects
            Set<String> unusedProjects = migrationAnalyzer.findUnusedProjects();
            report.setUnusedProjects(unusedProjects);
            logger.info("Found {} unused projects", unusedProjects.size());
            
            // Find circular dependencies
            List<List<String>> circularDeps = migrationAnalyzer.findCircularDependencies();
            report.setCircularDependencies(circularDeps);
            logger.info("Found {} circular dependency chains", circularDeps.size());
            
            // Calculate cohesion scores for all repositories
            Map<String, Double> cohesionScores = calculateRepositoryCohesion(allProjects, migrationAnalyzer);
            report.setProjectCohesionScores(cohesionScores);
            
            return report;
            
        } catch (Exception e) {
            logger.error("Error analyzing migration", e);
            throw new RuntimeException("Migration analysis failed: " + e.getMessage());
        }
    }
    
    @GetMapping("/unused-dependencies")
    public Map<String, Object> findUnusedDependencies() {
        try {
            Path rootPath = Paths.get("test-projects");
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            List<Project> allProjects = new ArrayList<>();
            List<Path> gitRepositories = analyzer.findGitRepositories(rootPath);
            for (Path repo : gitRepositories) {
                allProjects.addAll(analyzer.analyzeRepository(repo));
            }
            
            MigrationAnalyzer migrationAnalyzer = new MigrationAnalyzer(allProjects);
            Set<String> unusedProjects = migrationAnalyzer.findUnusedProjects();
            
            // Group by repository
            Map<String, List<String>> unusedByRepo = new HashMap<>();
            for (String projectName : unusedProjects) {
                Project project = allProjects.stream()
                    .filter(p -> p.getFullName().equals(projectName))
                    .findFirst()
                    .orElse(null);
                
                if (project != null) {
                    String repo = extractRepositoryName(project.getProjectPath());
                    unusedByRepo.computeIfAbsent(repo, k -> new ArrayList<>()).add(projectName);
                }
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalUnused", unusedProjects.size());
            result.put("unusedByRepository", unusedByRepo);
            result.put("details", unusedProjects);
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error finding unused dependencies", e);
            throw new RuntimeException("Failed to find unused dependencies: " + e.getMessage());
        }
    }
    
    @GetMapping("/circular-dependencies")
    public Map<String, Object> findCircularDependencies() {
        try {
            Path rootPath = Paths.get("test-projects");
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            List<Project> allProjects = new ArrayList<>();
            List<Path> gitRepositories = analyzer.findGitRepositories(rootPath);
            for (Path repo : gitRepositories) {
                allProjects.addAll(analyzer.analyzeRepository(repo));
            }
            
            MigrationAnalyzer migrationAnalyzer = new MigrationAnalyzer(allProjects);
            List<List<String>> cycles = migrationAnalyzer.findCircularDependencies();
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalCycles", cycles.size());
            result.put("cycles", cycles.stream()
                .map(cycle -> {
                    Map<String, Object> cycleInfo = new HashMap<>();
                    cycleInfo.put("projects", cycle);
                    cycleInfo.put("length", cycle.size() - 1); // Exclude duplicate
                    return cycleInfo;
                })
                .collect(Collectors.toList()));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error finding circular dependencies", e);
            throw new RuntimeException("Failed to find circular dependencies: " + e.getMessage());
        }
    }
    
    
    private Map<String, Set<String>> groupProjectsByRepository(List<Project> projects) {
        Map<String, Set<String>> projectsByRepo = new HashMap<>();
        for (Project project : projects) {
            String repo = extractRepositoryName(project.getProjectPath());
            projectsByRepo.computeIfAbsent(repo, k -> new HashSet<>()).add(project.getFullName());
        }
        return projectsByRepo;
    }
    
    private int[] countDependencies(List<Project> allProjects, Set<String> repoProjects) {
        int internal = 0;
        int external = 0;
        
        Map<String, Project> projectMap = new HashMap<>();
        Map<String, Project> versionlessMap = new HashMap<>();
        for (Project p : allProjects) {
            // フルネームでマップに追加
            projectMap.put(p.getFullName(), p);
            String versionlessKey = p.getGroupId() + ":" + p.getArtifactId();
            // 同じキーが既に存在する場合はログ出力
            if (versionlessMap.containsKey(versionlessKey)) {
                logger.warn("Duplicate groupId:artifactId found: {} in {} and {}", 
                    versionlessKey, 
                    versionlessMap.get(versionlessKey).getProjectPath(), 
                    p.getProjectPath());
            } else {
                versionlessMap.put(versionlessKey, p);
            }
        }
        // versionlessMapの内容をprojectMapに追加
        projectMap.putAll(versionlessMap);
        
        // バージョンなしのプロジェクト名セットを作成
        Set<String> repoProjectsWithoutVersion = new HashSet<>();
        for (String fullName : repoProjects) {
            Project p = projectMap.get(fullName);
            if (p != null) {
                repoProjectsWithoutVersion.add(p.getGroupId() + ":" + p.getArtifactId());
            }
        }
        
        for (String projectName : repoProjects) {
            Project project = projectMap.get(projectName);
            if (project != null) {
                for (Dependency dep : project.getDependencies()) {
                    // まずバージョン付きで探す
                    String depFullName = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
                    Project depProject = projectMap.get(depFullName);
                    
                    // バージョン付きで見つからない場合はバージョンなしで探す
                    if (depProject == null) {
                        String depNameWithoutVersion = dep.getGroupId() + ":" + dep.getArtifactId();
                        depProject = projectMap.get(depNameWithoutVersion);
                    }
                    
                    if (depProject != null) {
                        // 依存先プロジェクトが同じリポジトリ内にあるかチェック
                        String depVersionlessName = depProject.getGroupId() + ":" + depProject.getArtifactId();
                        if (repoProjects.contains(depProject.getFullName()) || 
                            repoProjectsWithoutVersion.contains(depVersionlessName)) {
                            internal++;
                        } else {
                            external++;
                        }
                    }
                }
            }
        }
        
        return new int[]{internal, external};
    }
    
    private int countSingleProjectDependencies(List<Project> allProjects, String projectName) {
        Map<String, Project> projectMap = new HashMap<>();
        Map<String, Project> versionlessMap = new HashMap<>();
        for (Project p : allProjects) {
            // フルネームでマップに追加
            projectMap.put(p.getFullName(), p);
            String versionlessKey = p.getGroupId() + ":" + p.getArtifactId();
            // 同じキーが既に存在する場合はログ出力
            if (versionlessMap.containsKey(versionlessKey)) {
                logger.warn("Duplicate groupId:artifactId found: {} in {} and {}", 
                    versionlessKey, 
                    versionlessMap.get(versionlessKey).getProjectPath(), 
                    p.getProjectPath());
            } else {
                versionlessMap.put(versionlessKey, p);
            }
        }
        // versionlessMapの内容をprojectMapに追加
        projectMap.putAll(versionlessMap);
        
        Project project = projectMap.get(projectName);
        if (project == null) return 0;
        
        int count = 0;
        for (Dependency dep : project.getDependencies()) {
            // まずバージョン付きで探す
            String depFullName = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
            Project depProject = projectMap.get(depFullName);
            
            // バージョン付きで見つからない場合はバージョンなしで探す
            if (depProject == null) {
                String depNameWithoutVersion = dep.getGroupId() + ":" + dep.getArtifactId();
                depProject = projectMap.get(depNameWithoutVersion);
            }
            
            if (depProject != null) {
                count++;
            }
        }
        return count;
    }
    
    private Map<String, Double> calculateRepositoryCohesion(List<Project> projects, MigrationAnalyzer analyzer) {
        Map<String, Double> cohesionScores = new HashMap<>();
        
        // Group projects by repository
        Map<String, Set<String>> projectsByRepo = new HashMap<>();
        for (Project project : projects) {
            String repo = extractRepositoryName(project.getProjectPath());
            projectsByRepo.computeIfAbsent(repo, k -> new HashSet<>()).add(project.getFullName());
        }
        
        // Calculate cohesion for each repository
        for (Map.Entry<String, Set<String>> entry : projectsByRepo.entrySet()) {
            double cohesion = analyzer.calculateCohesion(entry.getValue());
            cohesionScores.put(entry.getKey(), cohesion);
        }
        
        return cohesionScores;
    }
    
    private Set<String> findDistinctRepositories(List<Project> projects) {
        return projects.stream()
            .map(p -> extractRepositoryName(p.getProjectPath()))
            .collect(Collectors.toSet());
    }
    
    private String extractRepositoryName(Path path) {
        String pathStr = path.toString();
        String[] parts = pathStr.split("/");
        // Find the repository name (usually the directory after test-projects)
        for (int i = 0; i < parts.length - 1; i++) {
            if (parts[i].equals("test-projects") && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return "unknown";
    }
    
}