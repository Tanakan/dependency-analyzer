package com.example.dependencies.analyzer.migration;

import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.Dependency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Analyzes projects for migration opportunities based on coupling and cohesion metrics
 */
public class MigrationAnalyzer {
    private static final Logger logger = LoggerFactory.getLogger(MigrationAnalyzer.class);
    
    private final Map<String, Project> projectMap;
    private final Map<String, Set<String>> dependencyGraph;
    private final Map<String, Set<String>> reverseDependencyGraph;
    
    public MigrationAnalyzer(List<Project> projects) {
        this.projectMap = new HashMap<>();
        // フルネームとバージョンなしの両方でプロジェクトをマップに追加
        // 同じgroupId:artifactIdが複数ある場合は、最初に見つかったものを優先
        Map<String, Project> versionlessMap = new HashMap<>();
        for (Project p : projects) {
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
        this.dependencyGraph = buildDependencyGraph(projects);
        this.reverseDependencyGraph = buildReverseDependencyGraph();
    }
    
    /**
     * Analyzes cohesion within a group of projects
     * High cohesion = projects in the group depend on each other more than external projects
     * For single-project repositories, cohesion = 0 if it has any external dependencies
     */
    public double calculateCohesion(Set<String> projectGroup) {
        int internalConnections = 0;
        int externalConnections = 0;
        
        for (String project : projectGroup) {
            Set<String> dependencies = dependencyGraph.getOrDefault(project, new HashSet<>());
            for (String dep : dependencies) {
                if (projectGroup.contains(dep)) {
                    internalConnections++;
                } else {
                    externalConnections++;
                }
            }
        }
        
        int totalConnections = internalConnections + externalConnections;
        if (totalConnections == 0) return 1.0; // No dependencies = perfect cohesion
        
        return (double) internalConnections / totalConnections;
    }
    
    /**
     * Analyzes coupling between two groups of projects
     * Low coupling = few dependencies between groups
     */
    public double calculateCoupling(Set<String> group1, Set<String> group2) {
        int connectionCount = 0;
        
        for (String project : group1) {
            Set<String> dependencies = dependencyGraph.getOrDefault(project, new HashSet<>());
            for (String dep : dependencies) {
                if (group2.contains(dep)) {
                    connectionCount++;
                }
            }
        }
        
        for (String project : group2) {
            Set<String> dependencies = dependencyGraph.getOrDefault(project, new HashSet<>());
            for (String dep : dependencies) {
                if (group1.contains(dep)) {
                    connectionCount++;
                }
            }
        }
        
        int maxPossibleConnections = group1.size() * group2.size() * 2;
        if (maxPossibleConnections == 0) return 0.0;
        
        return (double) connectionCount / maxPossibleConnections;
    }
    
    /**
     * Finds strongly connected components (potential repository boundaries)
     */
    public List<Set<String>> findStronglyConnectedComponents() {
        // Using Tarjan's algorithm for SCC
        Map<String, Integer> index = new HashMap<>();
        Map<String, Integer> lowlink = new HashMap<>();
        Map<String, Boolean> onStack = new HashMap<>();
        Stack<String> stack = new Stack<>();
        List<Set<String>> sccs = new ArrayList<>();
        int[] indexCounter = {0};
        
        for (String project : projectMap.keySet()) {
            if (!index.containsKey(project)) {
                strongConnect(project, index, lowlink, onStack, stack, sccs, indexCounter);
            }
        }
        
        return sccs;
    }
    
    /**
     * Identifies unused projects (no incoming dependencies)
     */
    public Set<String> findUnusedProjects() {
        Set<String> unused = new HashSet<>();
        
        for (String project : projectMap.keySet()) {
            Set<String> dependents = reverseDependencyGraph.getOrDefault(project, new HashSet<>());
            // Remove self-dependencies
            dependents.remove(project);
            
            if (dependents.isEmpty() && !isRootProject(project)) {
                unused.add(project);
            }
        }
        
        return unused;
    }
    
    
    /**
     * Identifies circular dependencies that need to be resolved before migration
     */
    public List<List<String>> findCircularDependencies() {
        List<List<String>> cycles = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        
        for (String project : projectMap.keySet()) {
            if (!visited.contains(project)) {
                List<String> currentPath = new ArrayList<>();
                findCyclesUtil(project, visited, recursionStack, currentPath, cycles);
            }
        }
        
        return cycles;
    }
    
    private void findCyclesUtil(String node, Set<String> visited, Set<String> recursionStack,
                                List<String> currentPath, List<List<String>> cycles) {
        visited.add(node);
        recursionStack.add(node);
        currentPath.add(node);
        
        Set<String> dependencies = dependencyGraph.getOrDefault(node, new HashSet<>());
        for (String dep : dependencies) {
            if (!visited.contains(dep)) {
                findCyclesUtil(dep, visited, recursionStack, currentPath, cycles);
            } else if (recursionStack.contains(dep)) {
                // Found cycle
                int cycleStart = currentPath.indexOf(dep);
                if (cycleStart != -1) {
                    List<String> cycle = new ArrayList<>(currentPath.subList(cycleStart, currentPath.size()));
                    cycle.add(dep); // Complete the cycle
                    cycles.add(cycle);
                }
            }
        }
        
        currentPath.remove(currentPath.size() - 1);
        recursionStack.remove(node);
    }
    
    
    private boolean isRootProject(String project) {
        Project p = projectMap.get(project);
        if (p == null) return false;
        
        // Only consider WAR files and specific application entry points as root projects
        if ("war".equals(p.getPackaging())) {
            return true;
        }
        
        // Check for specific patterns that indicate a root/entry point project
        String artifactId = p.getArtifactId();
        return artifactId.endsWith("-application") ||
               artifactId.endsWith("-webapp") ||
               artifactId.endsWith("-war") ||
               artifactId.equals("main") ||
               artifactId.equals("app");
    }
    
    private void strongConnect(String v, Map<String, Integer> index, Map<String, Integer> lowlink,
                              Map<String, Boolean> onStack, Stack<String> stack, 
                              List<Set<String>> sccs, int[] indexCounter) {
        index.put(v, indexCounter[0]);
        lowlink.put(v, indexCounter[0]);
        indexCounter[0]++;
        stack.push(v);
        onStack.put(v, true);
        
        Set<String> dependencies = dependencyGraph.getOrDefault(v, new HashSet<>());
        for (String w : dependencies) {
            if (!index.containsKey(w)) {
                strongConnect(w, index, lowlink, onStack, stack, sccs, indexCounter);
                lowlink.put(v, Math.min(lowlink.get(v), lowlink.get(w)));
            } else if (onStack.getOrDefault(w, false)) {
                lowlink.put(v, Math.min(lowlink.get(v), index.get(w)));
            }
        }
        
        if (lowlink.get(v).equals(index.get(v))) {
            Set<String> scc = new HashSet<>();
            String w;
            do {
                w = stack.pop();
                onStack.put(w, false);
                scc.add(w);
            } while (!v.equals(w));
            
            if (scc.size() > 1) { // Only interested in non-trivial SCCs
                sccs.add(scc);
            }
        }
    }
    
    private Map<String, Set<String>> buildDependencyGraph(List<Project> projects) {
        Map<String, Set<String>> graph = new HashMap<>();
        
        for (Project project : projects) {
            String projectName = project.getFullName();
            Set<String> deps = new HashSet<>();
            
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
                    // グラフにはフルネーム（バージョン付き）を追加
                    deps.add(depProject.getFullName());
                }
            }
            
            graph.put(projectName, deps);
        }
        
        return graph;
    }
    
    private Map<String, Set<String>> buildReverseDependencyGraph() {
        Map<String, Set<String>> reverseGraph = new HashMap<>();
        
        for (Map.Entry<String, Set<String>> entry : dependencyGraph.entrySet()) {
            String project = entry.getKey();
            for (String dep : entry.getValue()) {
                reverseGraph.computeIfAbsent(dep, k -> new HashSet<>()).add(project);
            }
        }
        
        return reverseGraph;
    }
}