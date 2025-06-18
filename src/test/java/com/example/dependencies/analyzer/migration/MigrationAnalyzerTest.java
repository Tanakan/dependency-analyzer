package com.example.dependencies.analyzer.migration;

import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.ProjectType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class MigrationAnalyzerTest {
    
    private List<Project> testProjects;
    private MigrationAnalyzer analyzer;
    
    @BeforeEach
    void setUp() {
        testProjects = new ArrayList<>();
        
        // リポジトリ1: ecommerce-platform
        Project ecomCore = new Project(
            "com.example.ecommerce",
            "ecommerce-core",
            "1.0.0",
            Paths.get("test-projects/ecommerce-platform/ecommerce-core"),
            ProjectType.MAVEN
        );
        ecomCore.setPackaging("jar");
        
        Project ecomApi = new Project(
            "com.example.ecommerce",
            "ecommerce-api",
            "1.0.0",
            Paths.get("test-projects/ecommerce-platform/ecommerce-api"),
            ProjectType.MAVEN
        );
        ecomApi.setPackaging("jar");
        Dependency apiToCore = new Dependency(
            "com.example.ecommerce",
            "ecommerce-core",
            "1.0.0",
            "compile"
        );
        ecomApi.addDependency(apiToCore);
        
        Project ecomWeb = new Project(
            "com.example.ecommerce",
            "ecommerce-web",
            "1.0.0",
            Paths.get("test-projects/ecommerce-platform/ecommerce-web"),
            ProjectType.MAVEN
        );
        ecomWeb.setPackaging("war");
        Dependency webToApi = new Dependency(
            "com.example.ecommerce",
            "ecommerce-api",
            "1.0.0",
            "compile"
        );
        ecomWeb.addDependency(webToApi);
        
        // リポジトリ2: shared-utils (他のリポジトリから参照される)
        Project sharedUtils = new Project(
            "com.example.shared",
            "shared-utils",
            "1.0.0",
            Paths.get("test-projects/shared-utils"),
            ProjectType.MAVEN
        );
        sharedUtils.setPackaging("jar");
        
        // リポジトリ3: order-service (sharedを参照)
        Project orderService = new Project(
            "com.example.order",
            "order-service",
            "1.0.0",
            Paths.get("test-projects/order-service"),
            ProjectType.MAVEN
        );
        orderService.setPackaging("jar");
        Dependency orderToShared = new Dependency(
            "com.example.shared",
            "shared-utils",
            "1.0.0",
            "compile"
        );
        orderService.addDependency(orderToShared);
        
        testProjects.addAll(Arrays.asList(ecomCore, ecomApi, ecomWeb, sharedUtils, orderService));
        analyzer = new MigrationAnalyzer(testProjects);
    }
    
    @Test
    void testCalculateCohesion_SingleProject() {
        Set<String> singleProject = new HashSet<>();
        singleProject.add("com.example.shared:shared-utils:1.0.0");
        
        double cohesion = analyzer.calculateCohesion(singleProject);
        assertEquals(1.0, cohesion, "Single project should have cohesion of 1.0");
    }
    
    @Test
    void testCalculateCohesion_HighCohesion() {
        // ecommerce-platformの3プロジェクト（内部依存が強い）
        Set<String> ecommerceProjects = new HashSet<>();
        ecommerceProjects.add("com.example.ecommerce:ecommerce-core:1.0.0");
        ecommerceProjects.add("com.example.ecommerce:ecommerce-api:1.0.0");
        ecommerceProjects.add("com.example.ecommerce:ecommerce-web:1.0.0");
        
        double cohesion = analyzer.calculateCohesion(ecommerceProjects);
        // ecommerce-api -> ecommerce-core (内部)
        // ecommerce-web -> ecommerce-api (内部)
        // 内部依存: 2, 外部依存: 0
        assertEquals(1.0, cohesion, 0.01, "Projects with only internal dependencies should have high cohesion");
    }
    
    @Test
    void testCalculateCohesion_LowCohesion() {
        // order-serviceとecommerce-coreを無理やり同じリポジトリとした場合
        Set<String> mixedProjects = new HashSet<>();
        mixedProjects.add("com.example.order:order-service:1.0.0");
        mixedProjects.add("com.example.ecommerce:ecommerce-core:1.0.0");
        
        double cohesion = analyzer.calculateCohesion(mixedProjects);
        // order-service -> shared-utils (外部)
        // 内部依存: 0, 外部依存: 1
        assertEquals(0.0, cohesion, 0.01, "Projects with only external dependencies should have zero cohesion");
    }
    
    @Test
    void testFindUnusedProjects() {
        Set<String> unused = analyzer.findUnusedProjects();
        
        // ecommerce-coreは内部から参照されている
        assertFalse(unused.contains("com.example.ecommerce:ecommerce-core:1.0.0"), 
            "ecommerce-core should not be unused (referenced by ecommerce-api)");
        
        // ecommerce-webはWARなので除外
        assertFalse(unused.contains("com.example.ecommerce:ecommerce-web:1.0.0"), 
            "WAR files should not be marked as unused");
        
        // order-serviceは誰からも参照されていない
        assertTrue(unused.contains("com.example.order:order-service:1.0.0"), 
            "order-service should be unused (not referenced by any project)");
    }
    
    @Test
    void testCalculateCoupling() {
        Set<String> group1 = new HashSet<>();
        group1.add("com.example.ecommerce:ecommerce-core:1.0.0");
        group1.add("com.example.ecommerce:ecommerce-api:1.0.0");
        
        Set<String> group2 = new HashSet<>();
        group2.add("com.example.order:order-service:1.0.0");
        
        double coupling = analyzer.calculateCoupling(group1, group2);
        // group1とgroup2間には依存関係がない
        assertEquals(0.0, coupling, 0.01, "Groups with no dependencies should have zero coupling");
    }
    
    @Test
    void testCircularDependencies() {
        // 循環依存のテストプロジェクトを追加
        Project serviceA = new Project(
            "com.example.circular",
            "service-a",
            "1.0.0",
            Paths.get("test-projects/circular/service-a"),
            ProjectType.MAVEN
        );
        serviceA.setPackaging("jar");
        
        Project serviceB = new Project(
            "com.example.circular",
            "service-b",
            "1.0.0",
            Paths.get("test-projects/circular/service-b"),
            ProjectType.MAVEN
        );
        serviceB.setPackaging("jar");
        
        // A -> B
        Dependency aToB = new Dependency(
            "com.example.circular",
            "service-b",
            "1.0.0",
            "compile"
        );
        serviceA.addDependency(aToB);
        
        // B -> A (循環)
        Dependency bToA = new Dependency(
            "com.example.circular",
            "service-a",
            "1.0.0",
            "compile"
        );
        serviceB.addDependency(bToA);
        
        List<Project> projectsWithCircular = new ArrayList<>(testProjects);
        projectsWithCircular.add(serviceA);
        projectsWithCircular.add(serviceB);
        
        MigrationAnalyzer analyzerWithCircular = new MigrationAnalyzer(projectsWithCircular);
        List<List<String>> cycles = analyzerWithCircular.findCircularDependencies();
        
        assertTrue(cycles.size() > 0, "Should detect circular dependencies");
        
        // 循環が検出されたことを確認
        boolean foundCircular = false;
        for (List<String> cycle : cycles) {
            if (cycle.contains("com.example.circular:service-a:1.0.0") && 
                cycle.contains("com.example.circular:service-b:1.0.0")) {
                foundCircular = true;
                break;
            }
        }
        assertTrue(foundCircular, "Should detect the A->B->A circular dependency");
    }
}