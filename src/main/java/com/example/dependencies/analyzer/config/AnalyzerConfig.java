package com.example.dependencies.analyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "analyzer")
public class AnalyzerConfig {
    
    private Output output = new Output();
    private Scan scan = new Scan();
    
    public Output getOutput() {
        return output;
    }
    
    public void setOutput(Output output) {
        this.output = output;
    }
    
    public Scan getScan() {
        return scan;
    }
    
    public void setScan(Scan scan) {
        this.scan = scan;
    }
    
    public static class Output {
        private String directory = "./output";
        private String filename = "dependencies-analysis.json";
        private boolean createBackup = true;
        
        public String getDirectory() {
            return directory;
        }
        
        public void setDirectory(String directory) {
            this.directory = directory;
        }
        
        public String getFilename() {
            return filename;
        }
        
        public void setFilename(String filename) {
            this.filename = filename;
        }
        
        public boolean isCreateBackup() {
            return createBackup;
        }
        
        public void setCreateBackup(boolean createBackup) {
            this.createBackup = createBackup;
        }
        
        public String getFullPath() {
            return directory + "/" + filename;
        }
    }
    
    public static class Scan {
        private boolean includePomProjects = false;
        private int maxDepth = 10;
        
        public boolean isIncludePomProjects() {
            return includePomProjects;
        }
        
        public void setIncludePomProjects(boolean includePomProjects) {
            this.includePomProjects = includePomProjects;
        }
        
        public int getMaxDepth() {
            return maxDepth;
        }
        
        public void setMaxDepth(int maxDepth) {
            this.maxDepth = maxDepth;
        }
    }
}