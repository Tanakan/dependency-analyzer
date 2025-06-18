package com.example.dependencies.analyzer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DependencyAnalyzerTest {
    
    @TempDir
    Path tempDir;
    
    @Test
    void shouldThrowExceptionForInvalidDirectory() {
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        
        assertThatThrownBy(() -> analyzer.analyze("/non/existent/path"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid directory path");
    }
    
    @Test
    void shouldAnalyzeEmptyDirectory() throws IOException {
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        
        // Should not throw exception for empty directory
        analyzer.analyze(tempDir.toString());
    }
    
    @Test
    void shouldFindGitRepository() throws IOException {
        // Create a .git directory
        Path gitDir = tempDir.resolve(".git");
        Files.createDirectory(gitDir);
        
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        analyzer.analyze(tempDir.toString());
        
        // Test passes if no exception is thrown
    }
}