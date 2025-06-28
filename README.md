# Dependency Analyzer

A powerful tool for analyzing and visualizing dependencies in Maven and Gradle projects.

## Features

- 📊 **Interactive Visualization**: D3.js-based force-directed graph for exploring project dependencies
- 🔍 **Multi-Build Support**: Analyzes both Maven (pom.xml) and Gradle (build.gradle) projects
- 📈 **Cohesion Analysis**: Measures repository cohesion based on internal vs external dependencies
- ⚠️ **Issue Detection**: Identifies circular dependencies, unreferenced projects, and duplicate artifact IDs
- 🎯 **Smart Filtering**: Filter by individual projects or entire repositories with automatic zoom
- ⚛️ **React Frontend**: Modern UI that automatically loads analysis results

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Node.js 18+ (for React frontend)

### Installation & Usage

```bash
# 1. Clone and install
git clone https://github.com/Tanakan/dependency-analyzer.git
cd dependency-analyzer
mvn clean install

# 2. Analyze your projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/your-projects

# 3. View results with React
cd frontend
npm install
npm start
# Opens at http://localhost:3000 and automatically loads the analysis
```

That's it! The React app will automatically load `frontend/public/dependencies-analysis.json` when it starts.

### Detailed Usage

#### Step 1: Install the Analyzer

```bash
# Clone the repository
git clone https://github.com/Tanakan/dependency-analyzer.git
cd dependency-analyzer

# Install with Maven
mvn clean install
```

This creates an executable JAR in `target/dependencies-analyzer-1.0-SNAPSHOT.jar`.

#### Step 2: Analyze Your Projects

```bash
# Analyze a directory containing your projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <directory-path>

# Example: Analyze test projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar test-projects

# The analysis creates: frontend/public/dependencies-analysis.json
```

#### Step 3: View with React Frontend

```bash
# First time setup
cd frontend
npm install

# Start the React app
npm start
```

The React app will:
- Start at http://localhost:3000
- Automatically load the analysis from `public/dependencies-analysis.json`
- Display an interactive dependency graph

### Alternative: Static HTML Viewer

If you prefer not to use the React frontend, you can use the static HTML viewers:

```bash
# Open the static viewer
open src/main/resources/static/simple-graph.html

# Then manually load the generated JSON file
```

## Visualization Features

### Dependency Graph

- **Interactive force-directed graph** showing all project dependencies
- **Visual differentiation**:
  - JAR projects: Blue circles
  - WAR projects: Red squares
  - Repository grouping with labeled boundaries
- **Interactive features**:
  - Drag nodes to reorganize the layout
  - Click nodes to highlight dependency paths
  - Filter by repository or individual projects
  - Zoom and pan for large graphs

### Cohesion Analysis

- **Repository cohesion metrics** (internal dependencies ÷ total dependencies)
- **Visual indicators**:
  - Green (>70%): High cohesion - well-modularized repository
  - Yellow (40-70%): Medium cohesion - may need restructuring
  - Red (<40%): Low cohesion - consider splitting the repository
- **Detailed breakdown** of internal vs external dependencies
- **Unreferenced projects** identification

### Issues Detection

- **Circular Dependencies**: 
  - Visual representation of dependency cycles
  - Helps identify architectural problems
- **Unreferenced Projects**: 
  - Lists projects not used by any other project
  - Candidates for removal or separate deployment
- **Duplicate Artifact IDs**: 
  - Identifies naming conflicts across repositories
  - Helps maintain consistent project naming

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


## Development

### Dev Container Support

For VS Code users, this project includes a complete dev container configuration:

1. Install the "Dev Containers" extension in VS Code
2. Open the project folder
3. Click "Reopen in Container" when prompted
4. The container includes Java 17, Maven, Node.js 18, and all required tools

### Local Development

#### Building from Source
```bash
# Compile only
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn clean package

# Run without packaging
mvn compile exec:java -Dexec.mainClass="com.example.dependencies.analyzer.cli.DependencyAnalyzerCLI" \
  -Dexec.args="/path/to/projects"
```

#### Frontend Development (React)
```bash
cd frontend
npm install
npm start
# Opens at http://localhost:3000
```

The React frontend provides an alternative modern UI for viewing the analysis results.

## Additional Options

### Custom Output Location

```bash
# Specify a different output file
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/projects ./custom-output.json

# Use system properties
java -Danalyzer.output.directory=./results \
     -Danalyzer.output.filename=analysis.json \
     -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/projects
```

### Logging Control

```bash
# Quiet mode (only errors)
java -Dlogging.level.root=ERROR -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/projects

# Debug mode (detailed information)
java -Dlogging.level.root=DEBUG -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/projects
```

## Use Cases

### Monorepo Analysis
Analyze all projects in a monorepo to understand internal dependencies:
```bash
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/company/monorepo
```

### Microservices Architecture
Visualize dependencies across multiple service repositories:
```bash
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/microservices
```

### Legacy System Migration
Identify tightly coupled components before breaking apart a monolith:
```bash
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/legacy-system
```

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