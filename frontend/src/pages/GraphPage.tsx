import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Sidebar } from '../components/Sidebar/Sidebar';
import { Graph } from '../components/Graph/Graph';
import { GraphData } from '../types';
import '../App.css';

function GraphPage() {
  const [data, setData] = useState<GraphData | null>(null);
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [selectedRepository, setSelectedRepository] = useState<string | null>(null);

  useEffect(() => {
    fetch('/dependencies-analysis.json')
      .then(response => response.json())
      .then(data => {
        setData(data);
      })
      .catch(error => {
        console.error('Failed to load default data:', error);
      });
  }, []);

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
        <h1>Dependencies Analyzer</h1>
        <nav style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          {data && data.issues && (
            <Link to="/issues" className="nav-link">
              View Issues Analysis
            </Link>
          )}
        </nav>
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
            onNodeSelect={handleNodeSelect}
          />
        </div>
      ) : (
        <div className="app-placeholder">
          <h2>Loading Dependencies...</h2>
          <p>Please wait while we load the dependency analysis.</p>
        </div>
      )}
    </div>
  );
}

export default GraphPage;