import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { IssuesPanel } from '../components/IssuesPanel/IssuesPanel';
import { GraphData } from '../types';
import '../App.css';

function IssuesPage() {
  const [data, setData] = useState<GraphData | null>(null);

  useEffect(() => {
    // Load the same data file
    fetch('/dependencies-analysis.json')
      .then(response => response.json())
      .then((data: GraphData) => {
        setData(data);
      })
      .catch(error => console.error('Error loading data:', error));
  }, []);

  return (
    <div className="app">
      <div className="app-header">
        <h1>Project Issues Analysis</h1>
        <nav style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <Link to="/" className="nav-link">
            Back to Graph
          </Link>
        </nav>
      </div>
      
      <div className="issues-page-content" style={{ 
        padding: '60px',
        maxWidth: '2000px',
        margin: '0 auto',
        minHeight: 'calc(100vh - 100px)',
        backgroundColor: '#f9fafb'
      }}>
        {data && data.issues ? (
          <IssuesPanel issues={data.issues} />
        ) : (
          <div className="app-placeholder">
            <h2>Loading Issues Analysis...</h2>
            <p>Please wait while we load the analysis data.</p>
          </div>
        )}
      </div>
    </div>
  );
}

export default IssuesPage;