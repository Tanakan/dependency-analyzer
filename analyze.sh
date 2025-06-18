#!/bin/bash

# Simple script to run the dependency analyzer CLI

if [ $# -eq 0 ]; then
    echo "Usage: ./analyze.sh <directory-path>"
    exit 1
fi

# Compile and run the CLI directly
mvn compile exec:java -Dexec.mainClass="com.example.dependencies.analyzer.cli.DependencyAnalyzerCLI" -Dexec.args="$1" -Dexec.classpathScope=compile