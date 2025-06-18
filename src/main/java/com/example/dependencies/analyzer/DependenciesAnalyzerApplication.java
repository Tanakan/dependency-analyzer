package com.example.dependencies.analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DependenciesAnalyzerApplication {

    public static void main(String[] args) throws Exception {
        // If a directory argument is provided, run in CLI mode
        if (args.length > 0 && !args[0].startsWith("--")) {
            System.out.println("Running in CLI mode...");
            DependencyAnalyzer analyzer = new DependencyAnalyzer();
            analyzer.analyze(args[0]);
            System.exit(0);
        } else {
            // Otherwise, start the web application
            SpringApplication.run(DependenciesAnalyzerApplication.class, args);
        }
    }
}