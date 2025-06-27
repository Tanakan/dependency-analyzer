import React, { useState } from 'react';
import { IssuesData } from '../../types';
import './IssuesPanel.css';

interface IssuesPanelProps {
  issues?: IssuesData;
}

export const IssuesPanel: React.FC<IssuesPanelProps> = ({ issues }) => {
  const [expandedSections, setExpandedSections] = useState<Record<string, boolean>>({
    circular: true,
    unreferenced: false,
    duplicateArtifacts: false,
    duplicateGAVs: false
  });

  if (!issues) {
    return null;
  }

  const toggleSection = (section: string) => {
    setExpandedSections(prev => ({
      ...prev,
      [section]: !prev[section]
    }));
  };

  const circularCount = issues.circularReferences.length;
  const unreferencedCount = issues.unreferencedProjects.length;
  const duplicateArtifactCount = Object.keys(issues.duplicateArtifactIds).length;
  const duplicateGAVCount = Object.keys(issues.duplicateGAVs).length;

  return (
    <div className="issues-panel">
      <h2>
        <span style={{ fontSize: '48px', marginRight: '16px' }}>⚠️</span>
        Project Issues Analysis
      </h2>
      
      <div className="issues-summary">
        <div className="summary-item">
          <span className="summary-label">Circular References:</span>
          <span className="summary-count error" data-testid="summary-count-circular">{circularCount}</span>
        </div>
        <div className="summary-item">
          <span className="summary-label">Unreferenced Projects:</span>
          <span className="summary-count warning" data-testid="summary-count-unreferenced">{unreferencedCount}</span>
        </div>
        <div className="summary-item">
          <span className="summary-label">Duplicate Artifact IDs:</span>
          <span className="summary-count info" data-testid="summary-count-duplicate-artifacts">{duplicateArtifactCount}</span>
        </div>
        <div className="summary-item">
          <span className="summary-label">Duplicate GAVs:</span>
          <span className="summary-count error" data-testid="summary-count-duplicate-gavs">{duplicateGAVCount}</span>
        </div>
      </div>

      <div className="issues-sections">
        {/* Circular References */}
        <div className="issue-section">
          <div 
            className="section-header"
            onClick={() => toggleSection('circular')}
          >
            <span className="section-toggle">{expandedSections.circular ? '▼' : '▶'}</span>
            <h3>Circular References ({circularCount})</h3>
          </div>
          {expandedSections.circular && (
            <div className="section-content">
              {circularCount === 0 ? (
                <p className="no-issues">No circular references found</p>
              ) : (
                <ul className="issue-list">
                  {issues.circularReferences.map((cycle, index) => (
                    <li key={index} className="issue-item error">
                      <span className="cycle-label">Cycle {index + 1}:</span>
                      <span className="cycle-path">{cycle.join(' → ')}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        {/* Unreferenced Projects */}
        <div className="issue-section">
          <div 
            className="section-header"
            onClick={() => toggleSection('unreferenced')}
          >
            <span className="section-toggle">{expandedSections.unreferenced ? '▼' : '▶'}</span>
            <h3>Unreferenced Projects ({unreferencedCount})</h3>
          </div>
          {expandedSections.unreferenced && (
            <div className="section-content">
              {unreferencedCount === 0 ? (
                <p className="no-issues">All projects are referenced</p>
              ) : (
                <ul className="issue-list">
                  {issues.unreferencedProjects.map((projectId, index) => (
                    <li key={index} className="issue-item warning">
                      {projectId}
                    </li>
                  ))}
                </ul>
              )}
            </div>
          )}
        </div>

        {/* Duplicate Artifact IDs */}
        <div className="issue-section">
          <div 
            className="section-header"
            onClick={() => toggleSection('duplicateArtifacts')}
          >
            <span className="section-toggle">{expandedSections.duplicateArtifacts ? '▼' : '▶'}</span>
            <h3>Duplicate Artifact IDs ({duplicateArtifactCount})</h3>
          </div>
          {expandedSections.duplicateArtifacts && (
            <div className="section-content">
              {duplicateArtifactCount === 0 ? (
                <p className="no-issues">No duplicate artifact IDs found</p>
              ) : (
                <div className="duplicate-list">
                  {Object.entries(issues.duplicateArtifactIds).map(([artifactId, projectIds]) => (
                    <div key={artifactId} className="duplicate-item">
                      <h4>{artifactId}</h4>
                      <ul>
                        {projectIds.map((projectId, index) => (
                          <li key={index}>{projectId}</li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Duplicate GAVs */}
        <div className="issue-section">
          <div 
            className="section-header"
            onClick={() => toggleSection('duplicateGAVs')}
          >
            <span className="section-toggle">{expandedSections.duplicateGAVs ? '▼' : '▶'}</span>
            <h3>Duplicate GAVs ({duplicateGAVCount})</h3>
          </div>
          {expandedSections.duplicateGAVs && (
            <div className="section-content">
              {duplicateGAVCount === 0 ? (
                <p className="no-issues">No duplicate GAVs found</p>
              ) : (
                <div className="duplicate-list">
                  {Object.entries(issues.duplicateGAVs).map(([gav, projectIds]) => (
                    <div key={gav} className="duplicate-item">
                      <h4>{gav}</h4>
                      <ul>
                        {projectIds.map((projectId, index) => (
                          <li key={index}>{projectId}</li>
                        ))}
                      </ul>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};