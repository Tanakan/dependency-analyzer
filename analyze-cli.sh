#!/bin/bash

# Check if directory argument is provided
if [ $# -eq 0 ]; then
    echo "Usage: $0 <directory-path>"
    exit 1
fi

# Check if JAR exists
if [ ! -f "target/dependencies-analyzer-1.0-SNAPSHOT.jar" ]; then
    echo "JAR file not found. Building project..."
    mvn package -DskipTests
fi

# Run the application in CLI mode
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar "$1"