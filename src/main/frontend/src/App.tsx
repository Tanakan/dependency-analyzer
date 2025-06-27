import React, { useState } from 'react';
import { FileUpload } from './components/FileUpload/FileUpload';
import { Sidebar } from './components/Sidebar/Sidebar';
import { Graph } from './components/Graph/Graph';
import { GraphData } from './types';
import './App.css';

function App() {
  const [data, setData] = useState<GraphData | null>(null);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [selectedRepository, setSelectedRepository] = useState<string | null>(null);

  const handleDataLoaded = (newData: GraphData) => {
    setData(newData);
    setSelectedNode(null);
    setSelectedRepository(null);
  };

  const handleNodeSelect = (nodeId: string | null) => {
    setSelectedNode(nodeId);
    setSelectedRepository(null);
  };

  const handleRepositorySelect = (repository: string | null) => {
    setSelectedRepository(repository);
    setSelectedNode(null);
  };

  return (
    <div className="app">
      <div className="app-header">
        <h1>Dependencies Analyzer - React Version</h1>
        <FileUpload onDataLoaded={handleDataLoaded} />
      </div>
      
      {data ? (
        <div className="app-content">
          <Sidebar 
            nodes={data.nodes} 
            onNodeSelect={handleNodeSelect}
            onRepositorySelect={handleRepositorySelect}
          />
          <Graph 
            data={data} 
            selectedNode={selectedNode}
            selectedRepository={selectedRepository}
          />
        </div>
      ) : (
        <div className="app-placeholder">
          <h2>Welcome to Dependencies Analyzer</h2>
          <p>Upload a JSON file or load the default analysis to get started.</p>
        </div>
      )}
    </div>
  );
}

export default App;
