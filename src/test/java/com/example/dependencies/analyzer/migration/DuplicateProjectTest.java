package com.example.dependencies.analyzer.migration;

import com.example.dependencies.analyzer.model.Project;
import com.example.dependencies.analyzer.model.Dependency;
import com.example.dependencies.analyzer.model.ProjectType;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DuplicateProjectTest {
    
    @Test
    void testDuplicateGroupIdArtifactId() {
        List<Project> projects = new ArrayList<>();
        
        // 同じgroupId:artifactIdを持つ2つのプロジェクト（異なるリポジトリ）
        Project commonLib1 = new Project(
            "com.example",
            "common-lib",
            "1.0.0",
            Paths.get("repo1/common-lib"),
            ProjectType.MAVEN
        );
        commonLib1.setPackaging("jar");
        
        Project commonLib2 = new Project(
            "com.example",
            "common-lib",
            "2.0.0",
            Paths.get("repo2/common-lib"),
            ProjectType.MAVEN
        );
        commonLib2.setPackaging("jar");
        
        // commonLib1を参照するプロジェクト
        Project service1 = new Project(
            "com.example",
            "service1",
            "1.0.0",
            Paths.get("repo1/service1"),
            ProjectType.MAVEN
        );
        service1.setPackaging("jar");
        Dependency depToCommon = new Dependency(
            "com.example",
            "common-lib",
            "1.0.0",
            "compile"
        );
        service1.addDependency(depToCommon);
        
        projects.addAll(Arrays.asList(commonLib1, commonLib2, service1));
        
        // MigrationAnalyzerを作成（警告ログが出力されるはず）
        MigrationAnalyzer analyzer = new MigrationAnalyzer(projects);
        
        // 未使用プロジェクトの確認
        Set<String> unused = analyzer.findUnusedProjects();
        
        // commonLib2は誰からも参照されていないので未使用
        assertTrue(unused.contains("com.example:common-lib:2.0.0"), 
            "Duplicate project (version 2.0.0) should be unused");
        
        // commonLib1は参照されているので未使用ではない
        assertFalse(unused.contains("com.example:common-lib:1.0.0"), 
            "Referenced project (version 1.0.0) should not be unused");
        
        // service1も未使用ではない（ルートプロジェクトではないが、テストのため）
        assertTrue(unused.contains("com.example:service1:1.0.0"), 
            "Service1 should be unused (not referenced by others)");
    }
    
    @Test
    void testCohesionWithDuplicates() {
        List<Project> projects = new ArrayList<>();
        
        // リポジトリ1のプロジェクト
        Project repo1Common = new Project(
            "com.example",
            "common",
            "1.0.0",
            Paths.get("repo1/common"),
            ProjectType.MAVEN
        );
        repo1Common.setPackaging("jar");
        
        Project repo1Service = new Project(
            "com.example",
            "service",
            "1.0.0",
            Paths.get("repo1/service"),
            ProjectType.MAVEN
        );
        repo1Service.setPackaging("jar");
        repo1Service.addDependency(new Dependency("com.example", "common", "1.0.0", "compile"));
        
        // リポジトリ2のプロジェクト（同じgroupId:artifactId）
        Project repo2Common = new Project(
            "com.example",
            "common",
            "2.0.0",
            Paths.get("repo2/common"),
            ProjectType.MAVEN
        );
        repo2Common.setPackaging("jar");
        
        Project repo2Service = new Project(
            "com.example",
            "service",
            "2.0.0",
            Paths.get("repo2/service"),
            ProjectType.MAVEN
        );
        repo2Service.setPackaging("jar");
        repo2Service.addDependency(new Dependency("com.example", "common", "2.0.0", "compile"));
        
        projects.addAll(Arrays.asList(repo1Common, repo1Service, repo2Common, repo2Service));
        
        MigrationAnalyzer analyzer = new MigrationAnalyzer(projects);
        
        // repo1の凝集度を計算
        Set<String> repo1Projects = new HashSet<>();
        repo1Projects.add("com.example:common:1.0.0");
        repo1Projects.add("com.example:service:1.0.0");
        
        double cohesion1 = analyzer.calculateCohesion(repo1Projects);
        // repo1内で完結しているので凝集度は1.0になるはず
        assertEquals(1.0, cohesion1, 0.01, "Repo1 should have high cohesion");
        
        // repo2の凝集度を計算
        Set<String> repo2Projects = new HashSet<>();
        repo2Projects.add("com.example:common:2.0.0");
        repo2Projects.add("com.example:service:2.0.0");
        
        double cohesion2 = analyzer.calculateCohesion(repo2Projects);
        // repo2内で完結しているので凝集度は1.0になるはず
        assertEquals(1.0, cohesion2, 0.01, "Repo2 should have high cohesion");
    }
}