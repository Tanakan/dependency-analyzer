import React, { useEffect, useCallback, useMemo } from 'react';
import ReactFlow, {
  Node as FlowNode,
  Edge,
  Controls,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType,
  Position,
  Handle,
  NodeProps,
  BackgroundVariant,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { GraphData, Node } from '../../types';
import './Graph.css';

// Custom node component for Maven/Gradle projects
const CustomNode: React.FC<NodeProps> = ({ data }) => {
  const isPom = data.packaging === 'pom';
  
  return (
    <div 
      className={`custom-node ${isPom ? 'pom-node' : 'jar-node'}`}
      style={{
        padding: '8px 12px',
        borderRadius: isPom ? '4px' : '16px',
        border: '1px solid',
        borderColor: data.borderColor || '#e5e7eb',
        backgroundColor: data.backgroundColor || '#fff',
        fontSize: '12px',
        fontWeight: '500',
        textAlign: 'center',
        minWidth: '100px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
      }}
    >
      <Handle type="target" position={Position.Top} id="target-top" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Right} id="target-right" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Bottom} id="target-bottom" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Left} id="target-left" style={{ visibility: 'hidden' }} />
      <div>{data.label}</div>
      <div style={{ fontSize: '10px', color: '#666', marginTop: '2px' }}>
        {data.version}
      </div>
      <Handle type="source" position={Position.Top} id="source-top" style={{ visibility: 'hidden' }} />
      <Handle type="source" position={Position.Right} id="source-right" style={{ visibility: 'hidden' }} />
      <Handle type="source" position={Position.Bottom} id="source-bottom" style={{ visibility: 'hidden' }} />
      <Handle type="source" position={Position.Left} id="source-left" style={{ visibility: 'hidden' }} />
    </div>
  );
};

// Custom group node component for repository frames
const GroupNode: React.FC<NodeProps> = ({ data }) => {
  return (
    <div style={{ padding: '10px 20px', width: '100%', height: '100%' }}>
      <div style={{
        fontSize: '18px',
        fontWeight: '700',
        color: data.color || '#374151',
        marginBottom: '10px',
        textAlign: 'left',
        letterSpacing: '0.5px',
        textTransform: 'uppercase',
      }}>
        {data.label}
      </div>
    </div>
  );
};

const nodeTypes = {
  custom: CustomNode,
  group: GroupNode,
};

interface GraphProps {
  data: GraphData | null;
  selectedNode: string | null;
  selectedRepository: string | null;
  onNodeSelect?: (nodeId: string | null) => void;
}

export const Graph: React.FC<GraphProps> = ({ data, selectedNode, selectedRepository, onNodeSelect }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);

  // Create a color map for repositories
  const repoColorMap = useMemo(() => {
    if (!data) return new Map();
    
    // Color palette for repositories
    const colorPalette = [
      '#6366f1', '#8b5cf6', '#3b82f6', '#0ea5e9', '#06b6d4', 
      '#14b8a6', '#10b981', '#84cc16', '#f59e0b', '#f97316'
    ];
    
    const repos = Array.from(new Set(data.nodes.map(n => n.nodeGroup)));
    const map = new Map<string, string>();
    repos.forEach((repo, index) => {
      map.set(repo, colorPalette[index % colorPalette.length]);
    });
    return map;
  }, [data]);

  useEffect(() => {
    if (!data) return;

    // Group nodes by repository for layout
    const nodesByRepo = new Map<string, Node[]>();
    data.nodes.forEach(node => {
      const repo = node.nodeGroup;
      if (!nodesByRepo.has(repo)) {
        nodesByRepo.set(repo, []);
      }
      nodesByRepo.get(repo)!.push(node);
    });

    // Calculate grid layout for repositories
    const repos = Array.from(nodesByRepo.entries());
    const cols = Math.ceil(Math.sqrt(repos.length));
    const repoSpacing = 50;
    const nodePadding = 40; // Padding around nodes
    const nodeWidth = 120;
    const nodeHeight = 80;
    const nodeSpacing = 20; // Space between nodes

    // Create React Flow nodes
    const flowNodes: FlowNode[] = [];
    const nodePositions = new Map<string, { x: number; y: number }>();

    repos.forEach(([repoName, repoNodes], repoIndex) => {
      // Calculate dimensions based on number of nodes
      const nodesPerRow = Math.min(Math.ceil(Math.sqrt(repoNodes.length)), 4); // Max 4 nodes per row
      const numRows = Math.ceil(repoNodes.length / nodesPerRow);
      
      // Calculate repository frame size based on content
      const repoWidth = nodePadding * 2 + nodesPerRow * nodeWidth + (nodesPerRow - 1) * nodeSpacing;
      const repoHeight = nodePadding * 2 + 40 + numRows * nodeHeight + (numRows - 1) * nodeSpacing; // +40 for title

      // Calculate position in grid
      const col = repoIndex % cols;
      const row = Math.floor(repoIndex / cols);
      
      // Simple grid positioning with dynamic sizing
      const maxRepoWidth = nodePadding * 2 + 4 * nodeWidth + 3 * nodeSpacing; // Max width for 4 nodes per row
      const maxRepoHeight = 400; // Reasonable max height
      
      const repoX = col * (maxRepoWidth + repoSpacing);
      const repoY = row * (maxRepoHeight + repoSpacing);

      // Add repository group node
      flowNodes.push({
        id: `group-${repoName}`,
        type: 'group',
        position: { x: repoX, y: repoY },
        data: { 
          label: repoName,
          color: repoColorMap.get(repoName),
        },
        style: {
          width: repoWidth,
          height: repoHeight,
          backgroundColor: 'rgba(240, 240, 240, 0.5)',
          border: `2px dashed ${repoColorMap.get(repoName)}`,
          borderRadius: '8px',
        },
      });

      // Position nodes within repository
      repoNodes.forEach((node, nodeIndex) => {
        const nodeCol = nodeIndex % nodesPerRow;
        const nodeRow = Math.floor(nodeIndex / nodesPerRow);
        // Use relative position for nodes within groups
        const x = nodePadding + nodeCol * (nodeWidth + nodeSpacing);
        const y = nodePadding + 40 + nodeRow * (nodeHeight + nodeSpacing); // +40 for title space

        // Store absolute position for edge routing
        nodePositions.set(node.id, { x: repoX + x, y: repoY + y });

        flowNodes.push({
          id: node.id,
          type: 'custom',
          position: { x, y }, // Relative position within parent
          data: {
            label: node.name,
            version: node.version,
            packaging: node.packaging,
            borderColor: repoColorMap.get(repoName),
            backgroundColor: selectedNode === node.id ? 
              `${repoColorMap.get(repoName)}20` : '#fff',
          },
          parentNode: `group-${repoName}`,
          extent: 'parent',
          draggable: true,
        });
      });
    });

    // Create React Flow edges
    const flowEdges: Edge[] = data.links.map((link, index) => {
      const sourceId = typeof link.source === 'string' ? link.source : (link.source as any).id;
      const targetId = typeof link.target === 'string' ? link.target : (link.target as any).id;

      // Get node positions to determine best connection points
      const sourcePos = nodePositions.get(sourceId);
      const targetPos = nodePositions.get(targetId);
      
      let sourceHandle = 'source-bottom';
      let targetHandle = 'target-top';
      
      if (sourcePos && targetPos) {
        const dx = targetPos.x - sourcePos.x;
        const dy = targetPos.y - sourcePos.y;
        
        // Determine best connection based on relative positions
        if (Math.abs(dx) > Math.abs(dy)) {
          // Horizontal connection is better
          if (dx > 0) {
            sourceHandle = 'source-right';
            targetHandle = 'target-left';
          } else {
            sourceHandle = 'source-left';
            targetHandle = 'target-right';
          }
        } else {
          // Vertical connection is better
          if (dy > 0) {
            sourceHandle = 'source-bottom';
            targetHandle = 'target-top';
          } else {
            sourceHandle = 'source-top';
            targetHandle = 'target-bottom';
          }
        }
      }

      return {
        id: `e${index}`,
        source: sourceId,
        target: targetId,
        sourceHandle,
        targetHandle,
        type: 'default',
        animated: false, // No animation
        style: {
          stroke: selectedNode === sourceId || selectedNode === targetId ? 
            '#4f46e5' : '#374151',
          strokeWidth: selectedNode === sourceId || selectedNode === targetId ? 4 : 3,
        },
        markerEnd: {
          type: MarkerType.ArrowClosed,
          color: selectedNode === sourceId || selectedNode === targetId ? 
            '#4f46e5' : '#374151',
          width: 25,
          height: 25,
        },
      };
    });

    setNodes(flowNodes);
    setEdges(flowEdges);
  }, [data, selectedNode, selectedRepository, repoColorMap, setNodes, setEdges]);

  // Calculate transitive dependencies
  const getTransitiveDependencies = useCallback((nodeId: string, links: any[]): Set<string> => {
    const dependencies = new Set<string>();
    const queue = [nodeId];
    
    while (queue.length > 0) {
      const current = queue.shift()!;
      dependencies.add(current);
      
      // Find all nodes that the current node depends on
      links.forEach(link => {
        const sourceId = typeof link.source === 'string' ? link.source : (link.source as any).id;
        const targetId = typeof link.target === 'string' ? link.target : (link.target as any).id;
        
        if (sourceId === current && !dependencies.has(targetId)) {
          queue.push(targetId);
        }
      });
    }
    
    return dependencies;
  }, []);

  // Highlight nodes based on selection
  useEffect(() => {
    if (!data) return;
    
    const transitiveDeps = selectedNode ? getTransitiveDependencies(selectedNode, data.links) : null;
    
    setNodes((nds) =>
      nds.map((node) => {
        if (node.type === 'custom') {
          const isSelected = node.id === selectedNode;
          const isInSelectedRepo = selectedRepository && 
            node.parentNode === `group-${selectedRepository}`;
          const isTransitiveDep = transitiveDeps && transitiveDeps.has(node.id);
          
          // Hide nodes that are not part of transitive dependencies when a node is selected
          const shouldHide = selectedNode && !isTransitiveDep;
          
          return {
            ...node,
            data: {
              ...node.data,
              backgroundColor: isSelected ? 
                `${node.data.borderColor}20` : 
                (isInSelectedRepo ? `${node.data.borderColor}10` : '#fff'),
            },
            style: {
              ...node.style,
              opacity: shouldHide ? 0 : (selectedRepository && !isInSelectedRepo ? 0.3 : 1),
              display: shouldHide ? 'none' : 'block',
            },
          };
        } else if (node.type === 'group') {
          // Check if this group has any visible nodes
          const hasVisibleNodes = nds.some(n => 
            n.parentNode === node.id && 
            n.type === 'custom' && 
            (!selectedNode || (transitiveDeps && transitiveDeps.has(n.id)))
          );
          
          return {
            ...node,
            style: {
              ...node.style,
              opacity: selectedNode && !hasVisibleNodes ? 0.1 : 1,
            },
          };
        }
        return node;
      })
    );

    setEdges((eds) =>
      eds.map((edge) => {
        const isHighlighted = edge.source === selectedNode || 
                            edge.target === selectedNode;
        const isPartOfTransitive = transitiveDeps && 
          (transitiveDeps.has(edge.source) && transitiveDeps.has(edge.target));
        
        // Hide edges that are not part of transitive dependencies
        const shouldHide = selectedNode && !isPartOfTransitive;
        
        return {
          ...edge,
          animated: false, // Remove animation
          style: {
            ...edge.style,
            stroke: isHighlighted ? '#4f46e5' : '#374151',
            strokeWidth: isHighlighted ? 4 : 3,
            opacity: shouldHide ? 0 : (selectedNode && !isHighlighted ? 0.3 : 1),
            display: shouldHide ? 'none' : 'block',
          },
          markerEnd: {
            type: MarkerType.ArrowClosed,
            color: isHighlighted ? '#4f46e5' : '#374151',
            width: 25,
            height: 25,
          },
        };
      })
    );
  }, [selectedNode, selectedRepository, data, getTransitiveDependencies, setNodes, setEdges]);

  const onNodeClick = useCallback((event: React.MouseEvent, node: FlowNode) => {
    if (node.type === 'custom' && onNodeSelect) {
      // If clicking the same node, deselect it
      if (selectedNode === node.id) {
        onNodeSelect(null);
      } else {
        onNodeSelect(node.id);
      }
    }
  }, [onNodeSelect, selectedNode]);

  return (
    <div style={{ width: '100%', height: '100%' }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.1}
        maxZoom={2}
      >
        <Background variant={BackgroundVariant.Dots} color="#e5e7eb" gap={20} size={1} />
        <Controls />
        <MiniMap 
          nodeColor={(node) => {
            if (node.type === 'group') {
              return 'rgba(240, 240, 240, 0.5)';
            }
            return node.data?.borderColor || '#667eea';
          }}
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
          }}
        />
      </ReactFlow>
    </div>
  );
};