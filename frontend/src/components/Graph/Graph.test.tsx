import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { Graph } from './Graph';
import { GraphData } from '../../types';

// Mock ReactFlow
jest.mock('reactflow', () => {
  const React = require('react');
  const MockReactFlow = ({ children, nodes, edges, onNodeClick }: any) => {
    return (
      <div data-testid="react-flow">
        <div data-testid="react-flow-nodes">
          {nodes.map((node: any) => (
            <div 
              key={node.id} 
              data-testid={`node-${node.id}`}
              onClick={(e) => onNodeClick && onNodeClick(e, node)}
              style={{ cursor: 'pointer' }}
            >
              {node.data.label} - {node.data.version}
            </div>
          ))}
        </div>
        <div data-testid="react-flow-edges">
          {edges.map((edge: any) => (
            <div key={edge.id} data-testid={`edge-${edge.id}`}>
              {edge.source} → {edge.target}
            </div>
          ))}
        </div>
        {children}
      </div>
    );
  };

  return {
    __esModule: true,
    default: MockReactFlow,
    Controls: () => <div data-testid="controls">Controls</div>,
    Background: () => <div data-testid="background">Background</div>,
    MiniMap: () => <div data-testid="minimap">MiniMap</div>,
    Handle: () => null,
    Position: { Top: 'top', Bottom: 'bottom' },
    MarkerType: { ArrowClosed: 'arrowClosed' },
    BackgroundVariant: { Dots: 'dots' },
    useNodesState: () => {
      const [nodes, setNodes] = React.useState([]);
      return [nodes, setNodes, jest.fn()];
    },
    useEdgesState: () => {
      const [edges, setEdges] = React.useState([]);
      return [edges, setEdges, jest.fn()];
    },
  };
});

describe('Graph Component', () => {
  const mockData: GraphData = {
    nodes: [
      {
        id: 'com.example:service-a',
        name: 'service-a',
        version: '1.0.0',
        group: 'com.example',
        type: 'Maven',
        packaging: 'jar',
        nodeGroup: 'repo-1'
      },
      {
        id: 'com.example:service-b',
        name: 'service-b',
        version: '2.0.0',
        group: 'com.example',
        type: 'Maven',
        packaging: 'pom',
        nodeGroup: 'repo-1'
      },
      {
        id: 'com.example:service-c',
        name: 'service-c',
        version: '3.0.0',
        group: 'com.example',
        type: 'Gradle',
        packaging: 'jar',
        nodeGroup: 'repo-2'
      }
    ],
    links: [
      {
        source: 'com.example:service-a',
        target: 'com.example:service-b',
        value: 1
      },
      {
        source: 'com.example:service-b',
        target: 'com.example:service-c',
        value: 1
      }
    ],
    stats: {
      totalProjects: 3,
      totalDependencies: 2
    }
  };

  it('renders without crashing', () => {
    render(<Graph data={null} selectedNode={null} selectedRepository={null} />);
    expect(screen.getByTestId('react-flow')).toBeInTheDocument();
  });

  it('renders nodes and edges when data is provided', async () => {
    render(<Graph data={mockData} selectedNode={null} selectedRepository={null} />);
    
    await waitFor(() => {
      // Check if nodes are rendered
      expect(screen.getByTestId('node-com.example:service-a')).toBeInTheDocument();
      expect(screen.getByTestId('node-com.example:service-b')).toBeInTheDocument();
      expect(screen.getByTestId('node-com.example:service-c')).toBeInTheDocument();
      
      // Check if edges are rendered
      expect(screen.getByTestId('edge-e0')).toBeInTheDocument();
      expect(screen.getByTestId('edge-e1')).toBeInTheDocument();
    });
  });

  it('displays node information correctly', async () => {
    render(<Graph data={mockData} selectedNode={null} selectedRepository={null} />);
    
    await waitFor(() => {
      expect(screen.getByText('service-a - 1.0.0')).toBeInTheDocument();
      expect(screen.getByText('service-b - 2.0.0')).toBeInTheDocument();
      expect(screen.getByText('service-c - 3.0.0')).toBeInTheDocument();
    });
  });

  it('renders controls, background, and minimap', () => {
    render(<Graph data={mockData} selectedNode={null} selectedRepository={null} />);
    
    expect(screen.getByTestId('controls')).toBeInTheDocument();
    expect(screen.getByTestId('background')).toBeInTheDocument();
    expect(screen.getByTestId('minimap')).toBeInTheDocument();
  });

  it('calls onNodeSelect when a node is clicked', async () => {
    const mockOnNodeSelect = jest.fn();
    render(
      <Graph 
        data={mockData} 
        selectedNode={null} 
        selectedRepository={null}
        onNodeSelect={mockOnNodeSelect}
      />
    );
    
    await waitFor(() => {
      const nodeA = screen.getByTestId('node-com.example:service-a');
      fireEvent.click(nodeA);
      expect(mockOnNodeSelect).toHaveBeenCalledWith('com.example:service-a');
    });
  });

  it('groups nodes by repository', async () => {
    render(<Graph data={mockData} selectedNode={null} selectedRepository={null} />);
    
    await waitFor(() => {
      // Check if group nodes are created
      expect(screen.getByTestId('node-group-repo-1')).toBeInTheDocument();
      expect(screen.getByTestId('node-group-repo-2')).toBeInTheDocument();
    });
  });

  it('handles empty data gracefully', () => {
    const emptyData: GraphData = {
      nodes: [],
      links: [],
      stats: {
        totalProjects: 0,
        totalDependencies: 0
      }
    };
    
    render(<Graph data={emptyData} selectedNode={null} selectedRepository={null} />);
    expect(screen.getByTestId('react-flow')).toBeInTheDocument();
  });

  it('displays correct edge connections', async () => {
    render(<Graph data={mockData} selectedNode={null} selectedRepository={null} />);
    
    await waitFor(() => {
      expect(screen.getByText('com.example:service-a → com.example:service-b')).toBeInTheDocument();
      expect(screen.getByText('com.example:service-b → com.example:service-c')).toBeInTheDocument();
    });
  });
});