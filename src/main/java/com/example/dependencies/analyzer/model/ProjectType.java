package com.example.dependencies.analyzer.model;

public enum ProjectType {
    MAVEN("Maven"),
    GRADLE("Gradle");

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}