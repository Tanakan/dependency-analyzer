import React, { useState, useMemo } from 'react';
import { Node, Repository } from '../../types';
import './Sidebar.css';

interface SidebarProps {
  nodes: Node[];
  onNodeSelect: (nodeId: string | null) => void;
  onRepositorySelect: (repository: string | null) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ nodes, onNodeSelect, onRepositorySelect }) => {
  const [expandedRepos, setExpandedRepos] = useState<Set<string>>(new Set());
  const [selectedNode, setSelectedNode] = useState<string | null>(null);
  const [selectedRepo, setSelectedRepo] = useState<string | null>(null);

  const repositories = useMemo(() => {
    const repoMap = new Map<string, Node[]>();
    
    nodes.forEach(node => {
      const repo = node.nodeGroup || 'default';
      if (!repoMap.has(repo)) {
        repoMap.set(repo, []);
      }
      repoMap.get(repo)!.push(node);
    });

    return Array.from(repoMap.entries()).map(([name, nodes]) => ({
      name,
      nodes,
      projectCount: nodes.length
    })).sort((a, b) => b.projectCount - a.projectCount);
  }, [nodes]);

  const toggleRepository = (repoName: string) => {
    const newExpanded = new Set(expandedRepos);
    if (newExpanded.has(repoName)) {
      newExpanded.delete(repoName);
    } else {
      newExpanded.add(repoName);
    }
    setExpandedRepos(newExpanded);
  };

  const handleNodeClick = (nodeId: string) => {
    if (selectedNode === nodeId) {
      setSelectedNode(null);
      onNodeSelect(null);
    } else {
      setSelectedNode(nodeId);
      setSelectedRepo(null);
      onNodeSelect(nodeId);
    }
  };

  const handleRepoClick = (repoName: string) => {
    if (selectedRepo === repoName) {
      setSelectedRepo(null);
      onRepositorySelect(null);
    } else {
      setSelectedRepo(repoName);
      setSelectedNode(null);
      onRepositorySelect(repoName);
    }
  };

  return (
    <div className="sidebar">
      <div className="sidebar-header">
        <h3>Repositories</h3>
        <div className="stats">
          Total: {repositories.length} repos, {nodes.length} projects
        </div>
      </div>

      <div className="repository-list">
        {repositories.map(repo => (
          <div key={repo.name} className="repository-group">
            <div 
              className={`repository-header ${selectedRepo === repo.name ? 'selected' : ''}`}
              onClick={() => handleRepoClick(repo.name)}
            >
              <span 
                className={`chevron ${expandedRepos.has(repo.name) ? '' : 'collapsed'}`}
                onClick={(e) => {
                  e.stopPropagation();
                  toggleRepository(repo.name);
                }}
              >
                ▼
              </span>
              <span className="repo-name">{repo.name}</span>
              <span className="project-count">{repo.projectCount}</span>
            </div>
            
            {expandedRepos.has(repo.name) && (
              <div className="project-list">
                {repo.nodes.map(node => (
                  <div
                    key={node.id}
                    className={`project-item ${selectedNode === node.id ? 'selected' : ''}`}
                    onClick={() => handleNodeClick(node.id)}
                  >
                    <span className="project-name">{node.name}</span>
                    <span className="project-version">{node.version}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
};