# Dependencies Analyzer Frontend

A React-based visualization tool for Maven and Gradle project dependencies.

## Overview

This frontend application visualizes the dependency relationships between Maven and Gradle projects across multiple Git repositories. It provides an interactive graph visualization using D3.js.

## Features

- Interactive force-directed graph visualization
- Repository grouping with visual frames
- Node selection and highlighting
- Dependency relationship arrows
- Upload custom JSON analysis files
- Responsive design with modern UI

## Prerequisites

- Node.js 16 or higher
- npm 8 or higher

## Installation

```bash
npm install
```

## Development

Start the development server:

```bash
npm start
```

The application will open at http://localhost:3000

## Build

Create a production build:

```bash
npm run build
```

The build artifacts will be stored in the `build/` directory.

## Usage

1. **Load Analysis Data**: 
   - Click "Load Default Analysis" to load the sample data
   - Or click "Upload JSON File" to load your own analysis

2. **Interact with the Graph**:
   - Click on nodes to highlight them and their connections
   - Drag nodes to reposition them
   - Click on repository names in the sidebar to highlight all nodes in that repository
   - Drag repository frames to move all nodes within

## Data Format

The application expects JSON data in the following format:

```json
{
  "nodes": [
    {
      "id": "com.example:project-name",
      "name": "project-name",
      "version": "1.0.0",
      "group": "com.example",
      "type": "Maven",
      "nodeGroup": "repository-name"
    }
  ],
  "links": [
    {
      "source": 0,
      "target": 1,
      "value": 1
    }
  ]
}
```

## Integration with CLI

This frontend is designed to work with the Dependencies Analyzer CLI tool. The CLI generates the JSON analysis file that this frontend visualizes.

To generate analysis data:
1. Run the CLI tool on your repositories
2. Copy the generated `dependencies-analysis.json` to the frontend
3. Load it in the UI

## Technologies

- React 19 with TypeScript
- D3.js for graph visualization
- CSS3 for styling