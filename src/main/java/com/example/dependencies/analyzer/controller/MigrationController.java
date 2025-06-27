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
    
    private Path getRootPath() {
        // Get from system property, environment variable, or default to current directory
        String path = System.getProperty("analysis.directory");
        if (path == null) {
            path = System.getenv("ANALYSIS_DIRECTORY");
        }
        if (path == null) {
            path = ".";
        }
        return Paths.get(path);
    }
    
    @GetMapping("/repository-cohesion")
    public Map<String, Object> getRepositoryCohesion() {
        try {
            Path rootPath = getRootPath();
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
                
                // 内部・外部依存関係のカウント
                int[] counts = countDependencies(allProjects, repoProjects);
                analysis.setInternalDependencies(counts[0]);
                analysis.setExternalDependencies(counts[1]);
                
                // 凝集度を計算（単一プロジェクトでも正しく計算）
                double cohesion = migrationAnalyzer.calculateCohesion(repoProjects);
                analysis.setCohesionScore(cohesion);
                
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
            Path rootPath = getRootPath();
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
            Path rootPath = getRootPath();
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
            Path rootPath = getRootPath();
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
    
    @GetMapping("/dependency-impact")
    public Map<String, Object> getDependencyImpact() {
        try {
            Path rootPath = getRootPath();
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            List<Project> allProjects = new ArrayList<>();
            List<Path> gitRepositories = analyzer.findGitRepositories(rootPath);
            for (Path repo : gitRepositories) {
                allProjects.addAll(analyzer.analyzeRepository(repo));
            }
            
            // Create a map to count incoming dependencies for each project
            Map<String, Set<String>> incomingDependencies = new HashMap<>();
            Map<String, Project> projectMap = new HashMap<>();
            
            // Build project map for quick lookup
            for (Project project : allProjects) {
                projectMap.put(project.getFullName(), project);
                // Also add without version for matching
                String versionlessKey = project.getGroupId() + ":" + project.getArtifactId();
                projectMap.put(versionlessKey, project);
            }
            
            // Analyze all dependencies to find incoming connections
            for (Project project : allProjects) {
                for (Dependency dep : project.getDependencies()) {
                    // Try to find the dependency project
                    String depFullName = dep.getGroupId() + ":" + dep.getArtifactId() + ":" + dep.getVersion();
                    Project depProject = projectMap.get(depFullName);
                    
                    // If not found with version, try without version
                    if (depProject == null) {
                        String depNameWithoutVersion = dep.getGroupId() + ":" + dep.getArtifactId();
                        depProject = projectMap.get(depNameWithoutVersion);
                    }
                    
                    // If we found the dependency project, record the incoming dependency
                    if (depProject != null) {
                        String depProjectFullName = depProject.getFullName();
                        incomingDependencies.computeIfAbsent(depProjectFullName, k -> new HashSet<>())
                            .add(project.getFullName());
                    }
                }
            }
            
            // Group dependencies by repository
            Map<String, Map<String, Object>> repositoryImpacts = new HashMap<>();
            
            for (Map.Entry<String, Set<String>> entry : incomingDependencies.entrySet()) {
                String projectFullName = entry.getKey();
                Set<String> dependents = entry.getValue();
                
                Project project = projectMap.get(projectFullName);
                if (project != null) {
                    String repoName = extractRepositoryName(project.getProjectPath());
                    
                    Map<String, Object> repoData = repositoryImpacts.computeIfAbsent(repoName, k -> {
                        Map<String, Object> data = new HashMap<>();
                        data.put("repository", repoName);
                        data.put("totalIncomingDependencies", 0);
                        data.put("projects", new ArrayList<Map<String, Object>>());
                        return data;
                    });
                    
                    // Update total count
                    int currentTotal = (int) repoData.get("totalIncomingDependencies");
                    repoData.put("totalIncomingDependencies", currentTotal + dependents.size());
                    
                    // Add project details
                    Map<String, Object> projectData = new HashMap<>();
                    projectData.put("projectName", project.getFullName());
                    projectData.put("shortName", project.getArtifactId());
                    projectData.put("incomingCount", dependents.size());
                    projectData.put("dependents", new ArrayList<>(dependents));
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> projects = (List<Map<String, Object>>) repoData.get("projects");
                    projects.add(projectData);
                }
            }
            
            // Convert to list and sort by total incoming dependencies
            List<Map<String, Object>> sortedRepositories = new ArrayList<>(repositoryImpacts.values());
            sortedRepositories.sort((a, b) -> {
                int countA = (int) a.get("totalIncomingDependencies");
                int countB = (int) b.get("totalIncomingDependencies");
                return Integer.compare(countB, countA); // Descending order
            });
            
            // Sort projects within each repository by incoming count
            for (Map<String, Object> repo : sortedRepositories) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> projects = (List<Map<String, Object>>) repo.get("projects");
                projects.sort((a, b) -> {
                    int countA = (int) a.get("incomingCount");
                    int countB = (int) b.get("incomingCount");
                    return Integer.compare(countB, countA); // Descending order
                });
            }
            
            // Calculate statistics
            int totalProjects = allProjects.size();
            int projectsWithDependents = incomingDependencies.size();
            int totalDependencyRelations = incomingDependencies.values().stream()
                .mapToInt(Set::size)
                .sum();
            
            Map<String, Object> result = new HashMap<>();
            result.put("repositories", sortedRepositories);
            result.put("statistics", Map.of(
                "totalProjects", totalProjects,
                "projectsWithDependents", projectsWithDependents,
                "totalDependencyRelations", totalDependencyRelations,
                "averageDependentsPerProject", projectsWithDependents > 0 ? 
                    (double) totalDependencyRelations / projectsWithDependents : 0
            ));
            
            return result;
            
        } catch (Exception e) {
            logger.error("Error analyzing dependency impact", e);
            throw new RuntimeException("Dependency impact analysis failed: " + e.getMessage());
        }
    }
    
    private String extractRepositoryName(Path path) {
        // Find the git repository root
        Path currentPath = path;
        while (currentPath != null) {
            if (currentPath.resolve(".git").toFile().exists()) {
                return currentPath.getFileName().toString();
            }
            currentPath = currentPath.getParent();
        }
        // If no git repository found, use the last directory name
        return path.getFileName() != null ? path.getFileName().toString() : "unknown";
    }
    
}