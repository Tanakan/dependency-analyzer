package com.example.dependencies.analyzer;

public class DependencyAnalyzerRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java DependencyAnalyzerRunner <directory>");
            System.exit(1);
        }
        
        DependencyAnalyzer analyzer = new DependencyAnalyzer();
        analyzer.analyze(args[0]);
    }
}