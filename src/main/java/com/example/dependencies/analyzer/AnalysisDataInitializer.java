package com.example.dependencies.analyzer;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.nio.file.Files;

@Component
public class AnalysisDataInitializer implements CommandLineRunner {
    private static final Logger logger = LoggerFactory.getLogger(AnalysisDataInitializer.class);
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Initializing dependency analysis data...");
        
        // Check if dependencies-analysis.json exists and is recent
        var jsonPath = Paths.get("dependencies-analysis.json");
        if (Files.exists(jsonPath)) {
            var lastModified = Files.getLastModifiedTime(jsonPath);
            var ageInMinutes = (System.currentTimeMillis() - lastModified.toMillis()) / 1000 / 60;
            logger.info("Found existing dependencies-analysis.json (age: {} minutes)", ageInMinutes);
            
            // If file is older than 5 minutes, regenerate
            if (ageInMinutes > 5) {
                logger.info("Analysis data is stale, regenerating...");
                runAnalysis();
            }
        } else {
            logger.info("No existing analysis data found, generating...");
            runAnalysis();
        }
    }
    
    private void runAnalysis() {
        try {
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            // Get the directory from system property or use current directory
            String directory = System.getProperty("analysis.directory", ".");
            analyzer.analyze(directory);
            logger.info("Dependency analysis completed successfully");
        } catch (Exception e) {
            logger.error("Failed to run dependency analysis", e);
        }
    }
}