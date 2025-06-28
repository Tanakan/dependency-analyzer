#!/bin/bash

echo "🚀 Setting up Dependencies Analyzer development environment..."

# Maven setup
echo "📦 Installing Maven dependencies..."
mvn clean install -DskipTests

# Frontend setup
echo "📦 Installing frontend dependencies..."
cd frontend
npm install

# Create necessary directories
echo "📁 Creating necessary directories..."
mkdir -p /home/vscode/.m2/repository

# Git configuration
echo "🔧 Configuring Git..."
git config --global --add safe.directory /workspaces/dependencies-analyzer

# Display helpful information
echo "
✅ Development environment setup complete!

Available commands:
- Backend: mvn spring-boot:run
- Frontend: cd frontend && npm start
- CLI: mvn compile && java -cp target/classes com.example.dependencies.analyzer.cli.DependencyAnalyzerCLI

Ports:
- Frontend: http://localhost:3000
- Backend: http://localhost:8080

Happy coding! 🎉
"