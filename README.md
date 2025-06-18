# Dependency Analyzer

A powerful tool for analyzing and visualizing dependencies in Maven and Gradle projects.

## Features

- 📊 **Interactive Visualization**: D3.js-based force-directed graph for exploring project dependencies
- 🔍 **Multi-Build Support**: Analyzes both Maven (pom.xml) and Gradle (build.gradle) projects
- 📈 **Cohesion Analysis**: Measures repository cohesion based on internal vs external dependencies
- ⚠️ **Issue Detection**: Identifies circular dependencies, unreferenced projects, and duplicate artifact IDs
- 🎯 **Smart Filtering**: Filter by individual projects or entire repositories with automatic zoom
- 🚀 **Spring Boot Web UI**: Modern web interface for easy interaction

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+

### Installation

1. Clone the repository:
```bash
git clone https://github.com/Tanakan/dependency-analyzer.git
cd dependency-analyzer
```

2. Build the project:
```bash
mvn clean package
```

### Usage

#### Web Interface (Recommended)

1. Start the Spring Boot application:
```bash
mvn spring-boot:run
```

2. Open your browser and navigate to:
```
http://localhost:8080
```

3. The application will automatically analyze projects in the included `test-projects` directory

#### Command Line Interface

```bash
# Analyze a directory and generate dependency-graph.html
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <directory-path>

# Example: Analyze the included test-projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar test-projects
```

This will generate a `dependency-graph.html` file that can be opened in your browser.

## Web Interface Features

### Dependency Graph (/)

- Interactive force-directed graph showing all project dependencies
- Different shapes for JAR (circle) and WAR (square) projects
- Color-coded by repository
- Click on any project to filter and zoom
- Click on repository headers to filter by repository

### Cohesion Analysis (/cohesion.html)

- Repository cohesion scores (internal dependencies / total dependencies)
- Visual bar chart with color-coded cohesion levels:
  - Green (>0.7): High cohesion - good modularity
  - Yellow (0.4-0.7): Medium cohesion - consider reviewing
  - Red (<0.4): Low cohesion - consider splitting repository
- List of unreferenced projects

### Issues Analysis (/issues.html)

- **Circular Dependencies**: Detects and displays dependency cycles
- **Unreferenced Projects**: Projects not used by any other project
- **Duplicate Artifact IDs**: Projects with the same artifact ID in different locations

## Project Structure

```
dependency-analyzer/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/dependencies/analyzer/
│   │   │       ├── analyzer/          # Analysis logic
│   │   │       ├── controller/        # REST controllers
│   │   │       ├── migration/         # Cohesion analysis
│   │   │       ├── model/            # Data models
│   │   │       ├── parser/           # Maven/Gradle parsers
│   │   │       └── visualizer/       # Graph generation
│   │   └── resources/
│   │       └── static/               # Web UI files
│   └── test/                         # Unit tests
└── test-projects/                    # Sample projects demonstrating various dependency scenarios
    ├── circular-deps-example/        # Circular dependency demonstration
    ├── ecommerce-platform/          # Multi-module Maven project
    ├── order-processing/            # Multi-module Gradle project
    ├── platform-core/               # Nested Gradle modules
    └── ... (30+ example projects)
```

## API Endpoints

- `GET /api/dependencies/data` - Get dependency graph data
- `GET /api/migration/repository-cohesion` - Get repository cohesion analysis
- `GET /api/issues/analysis` - Get project issues analysis

## Configuration

The application analyzes projects in the `test-projects` directory by default. To analyze a different directory:

1. Modify the path in `DependencyAnalyzerRunner.java`
2. Or use the CLI with a custom path

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Built with Spring Boot and D3.js
- Inspired by the need for better visibility into complex project dependencies