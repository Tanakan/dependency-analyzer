# Dependency Analyzer

A powerful tool for analyzing and visualizing dependencies in Maven and Gradle projects.

## Features

- 📊 **Interactive Visualization**: D3.js-based force-directed graph for exploring project dependencies
- 🔍 **Multi-Build Support**: Analyzes both Maven (pom.xml) and Gradle (build.gradle) projects
- 📈 **Cohesion Analysis**: Measures repository cohesion based on internal vs external dependencies
- ⚠️ **Issue Detection**: Identifies circular dependencies, unreferenced projects, and duplicate artifact IDs
- 🎯 **Smart Filtering**: Filter by individual projects or entire repositories with automatic zoom
- 🌐 **Static HTML Output**: View results directly in your browser without a server

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- A modern web browser (Chrome, Firefox, Safari, Edge)

### Quick Installation

```bash
# Clone the repository
git clone https://github.com/Tanakan/dependency-analyzer.git
cd dependency-analyzer

# Build the project
mvn clean package

# Test with sample projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar test-projects

# Open the visualization
open src/main/resources/static/simple-graph.html
```

### Usage

#### Step 1: Build the Project

```bash
# Clone the repository
git clone https://github.com/Tanakan/dependency-analyzer.git
cd dependency-analyzer

# Build with Maven
mvn clean package
```

#### Step 2: Analyze Your Projects

```bash
# Basic usage - analyze a directory containing Git repositories
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <directory-path>

# Example: Analyze the included test projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar test-projects

# Example: Analyze your own projects
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/my-projects

# Specify custom output location
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/my-projects ./my-analysis.json

# Use system properties for configuration
java -Danalyzer.output.directory=./results \
     -Danalyzer.output.filename=deps-$(date +%Y%m%d).json \
     -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/my-projects
```

The tool will:
1. Scan all Git repositories in the specified directory
2. Find all Maven (pom.xml) and Gradle (build.gradle/build.gradle.kts) projects
3. Analyze dependencies between projects
4. Generate a JSON file with the analysis results

#### Step 3: Visualize the Results

1. **Open the visualization page**:
   - Navigate to `src/main/resources/static/` in your file browser
   - Open `simple-graph.html` in a web browser
   - Or use a local web server:
     ```bash
     cd src/main/resources/static
     python3 -m http.server 8000
     # Then open http://localhost:8000/simple-graph.html
     ```

2. **Load your analysis**:
   - Click "Choose File" button
   - Select the generated JSON file (default: `./frontend/public/dependencies-analysis.json`)
   - The dependency graph will be displayed automatically

3. **Interact with the graph**:
   - **Drag nodes** to rearrange the layout
   - **Click on a node** to highlight its dependencies
   - **Click on repository labels** to filter by repository
   - **Use mouse wheel** to zoom in/out
   - **Double-click** on empty space to reset the view

#### Additional Visualizations

Besides the main dependency graph, you can also use:

- **`cohesion.html`** - Analyze repository cohesion (internal vs external dependencies)
- **`issues.html`** - View detected issues like circular dependencies and unreferenced projects

### Example Workflow

```bash
# 1. Build the tool
mvn clean package

# 2. Analyze your microservices
java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar ~/microservices-workspace

# 3. View the results
open src/main/resources/static/simple-graph.html
# Or on Linux: xdg-open src/main/resources/static/simple-graph.html

# 4. Load the generated dependencies-analysis.json file in the web interface
```

## Visualization Features

### Dependency Graph (simple-graph.html)

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

### Cohesion Analysis (cohesion.html)

- **Repository cohesion metrics** (internal dependencies ÷ total dependencies)
- **Visual indicators**:
  - Green (>70%): High cohesion - well-modularized repository
  - Yellow (40-70%): Medium cohesion - may need restructuring
  - Red (<40%): Low cohesion - consider splitting the repository
- **Detailed breakdown** of internal vs external dependencies
- **Unreferenced projects** identification

### Issues Detection (issues.html)

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

## Configuration Options

### Output Location

By default, the analysis results are saved to `./frontend/public/dependencies-analysis.json`.

You can customize this using:

1. **Command line argument**:
   ```bash
   java -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <input-dir> <output-file>
   ```

2. **System properties**:
   ```bash
   java -Danalyzer.output.directory=./results \
        -Danalyzer.output.filename=analysis.json \
        -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <input-dir>
   ```

### Logging Configuration

Control the verbosity of the analysis:

```bash
# Quiet mode (only errors)
java -Dlogging.level.root=ERROR -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <dir>

# Debug mode (detailed information)
java -Dlogging.level.root=DEBUG -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <dir>

# Specific package logging
java -Dlogging.level.com.example.dependencies.analyzer=DEBUG \
     -jar target/dependencies-analyzer-1.0-SNAPSHOT.jar <dir>
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