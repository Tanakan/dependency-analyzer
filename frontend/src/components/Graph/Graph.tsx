import React, { useEffect, useCallback } from 'react';
import ReactFlow, {
  Node as FlowNode,
  Edge,
  Background,
  MiniMap,
  useNodesState,
  useEdgesState,
  MarkerType,
  Position,
  Handle,
  NodeProps,
  BackgroundVariant,
  useReactFlow,
  Panel,
  ReactFlowProvider,
} from 'reactflow';
import 'reactflow/dist/style.css';
import { GraphData, Node } from '../../types';
import './Graph.css';

// Custom node component for Maven/Gradle projects
const CustomNode: React.FC<NodeProps> = ({ data }) => {
  const isPom = data.packaging === 'pom';
  const isWar = data.packaging === 'war';
  
  // Different styles for different packaging types
  const getNodeStyle = () => {
    if (isPom) {
      return {
        borderRadius: '0px',
        borderStyle: 'dashed' as const,
        borderWidth: '3px',
        borderColor: '#d97706',
        backgroundColor: data.backgroundColor || (data.selected ? '#f59e0b' : '#fbbf24'),
      };
    } else if (isWar) {
      return {
        borderRadius: '8px',
        borderStyle: 'solid' as const,
        borderWidth: '2px',
        borderColor: '#dc2626',
        backgroundColor: data.backgroundColor || (data.selected ? '#fca5a5' : '#fecaca'),
      };
    } else {
      // Default for jar and others
      return {
        borderRadius: '16px',
        borderStyle: 'solid' as const,
        borderWidth: '2px',
        borderColor: '#2563eb',
        backgroundColor: data.backgroundColor || (data.selected ? '#93c5fd' : '#bfdbfe'),
      };
    }
  };
  
  const nodeStyle = getNodeStyle();
  
  return (
    <div 
      className={`custom-node ${isPom ? 'pom-node' : isWar ? 'war-node' : 'jar-node'}`}
      style={{
        padding: '10px 15px',
        borderRadius: nodeStyle.borderRadius,
        border: `${nodeStyle.borderWidth} ${nodeStyle.borderStyle}`,
        borderColor: nodeStyle.borderColor,
        backgroundColor: nodeStyle.backgroundColor,
        fontSize: '12px',
        fontWeight: '500',
        textAlign: 'center',
        minWidth: '120px',
        boxShadow: '0 1px 3px rgba(0,0,0,0.08)',
        position: 'relative',
      }}
    >
      <Handle type="target" position={Position.Top} id="target-top" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Right} id="target-right" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Bottom} id="target-bottom" style={{ visibility: 'hidden' }} />
      <Handle type="target" position={Position.Left} id="target-left" style={{ visibility: 'hidden' }} />
      <div style={{ color: '#1f2937', fontWeight: '600' }}>{data.label}</div>
      <div style={{ fontSize: '10px', color: '#6b7280', marginTop: '2px' }}>
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
    <div style={{ 
      padding: '10px 15px', 
      width: '100%', 
      height: '100%',
      position: 'relative',
    }}>
      <div style={{
        position: 'absolute',
        top: '-18px',
        left: '15px',
        backgroundColor: '#374151',
        color: '#ffffff',
        padding: '4px 16px',
        borderRadius: '6px',
        fontSize: '16px',
        fontWeight: '700',
        textAlign: 'left',
        letterSpacing: '0.3px',
        boxShadow: '0 2px 6px rgba(0,0,0,0.25)',
        zIndex: 1,
        whiteSpace: 'nowrap',
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

// Custom zoom controls component
const CustomZoomControls: React.FC = () => {
  const { zoomIn, zoomOut, fitView } = useReactFlow();
  
  const handleZoomIn = () => {
    zoomIn({ duration: 200 });
  };
  
  const handleZoomOut = () => {
    zoomOut({ duration: 200 });
  };
  
  const handleFitView = () => {
    fitView({ duration: 400, padding: 0.2 });
  };
  
  return (
    <Panel position="top-right" style={{ marginTop: '10px', marginRight: '10px' }}>
      <div style={{ 
        display: 'flex', 
        flexDirection: 'column', 
        gap: '2px',
        background: 'white',
        border: '1px solid #e5e7eb',
        borderRadius: '6px',
        padding: '4px',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.1)',
      }}>
        <button
          onClick={handleZoomIn}
          style={{
            background: 'white',
            border: '1px solid #e5e7eb',
            borderRadius: '4px',
            width: '32px',
            height: '32px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '16px',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#f3f4f6';
            e.currentTarget.style.borderColor = '#d1d5db';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'white';
            e.currentTarget.style.borderColor = '#e5e7eb';
          }}
        >
          +
        </button>
        <button
          onClick={handleZoomOut}
          style={{
            background: 'white',
            border: '1px solid #e5e7eb',
            borderRadius: '4px',
            width: '32px',
            height: '32px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '16px',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#f3f4f6';
            e.currentTarget.style.borderColor = '#d1d5db';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'white';
            e.currentTarget.style.borderColor = '#e5e7eb';
          }}
        >
          −
        </button>
        <button
          onClick={handleFitView}
          style={{
            background: 'white',
            border: '1px solid #e5e7eb',
            borderRadius: '4px',
            width: '32px',
            height: '32px',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '12px',
            transition: 'all 0.2s ease',
          }}
          onMouseEnter={(e) => {
            e.currentTarget.style.background = '#f3f4f6';
            e.currentTarget.style.borderColor = '#d1d5db';
          }}
          onMouseLeave={(e) => {
            e.currentTarget.style.background = 'white';
            e.currentTarget.style.borderColor = '#e5e7eb';
          }}
        >
          ⊡
        </button>
      </div>
    </Panel>
  );
};

interface GraphProps {
  data: GraphData | null;
  selectedNode: string | null;
  selectedRepository: string | null;
  onNodeSelect?: (nodeId: string | null) => void;
}

const GraphContent: React.FC<GraphProps> = ({ data, selectedNode, selectedRepository, onNodeSelect }) => {
  const [nodes, setNodes, onNodesChange] = useNodesState([]);
  const [edges, setEdges, onEdgesChange] = useEdgesState([]);
  const { setViewport, getViewport } = useReactFlow();
  
  // Custom wheel handler for smoother zoom
  useEffect(() => {
    const handleWheel = (e: Event) => {
      const wheelEvent = e as WheelEvent;
      if (wheelEvent.ctrlKey || wheelEvent.metaKey) {
        wheelEvent.preventDefault();
        const viewport = getViewport();
        const zoomSpeed = 0.002; // Very small zoom increments
        const delta = -wheelEvent.deltaY * zoomSpeed;
        const newZoom = Math.max(0.2, Math.min(3, viewport.zoom + delta));
        
        setViewport({
          x: viewport.x,
          y: viewport.y,
          zoom: newZoom,
        });
      }
    };
    
    const container = document.querySelector('.react-flow');
    if (container) {
      container.addEventListener('wheel', handleWheel as EventListener, { passive: false });
      return () => {
        container.removeEventListener('wheel', handleWheel as EventListener);
      };
    }
  }, [getViewport, setViewport]);
  

  // Custom node change handler with collision detection
  const handleNodesChange = useCallback((changes: any[]) => {
    const nodeMap = new Map(nodes.map(n => [n.id, n]));
    
    // Filter position changes
    const positionChanges = changes.filter(change => change.type === 'position' && change.dragging);
    
    if (positionChanges.length > 0) {
      const modifiedChanges = changes.map(change => {
        if (change.type === 'position' && change.dragging && change.position) {
          const movingNode = nodeMap.get(change.id);
          if (!movingNode || movingNode.type !== 'custom') return change;
          
          const nodeWidth = 150;
          const nodeHeight = 80;
          const minDistance = 5; // Smaller minimum gap between nodes
          
          let newX = change.position.x;
          let newY = change.position.y;
          
          // Check collision with other nodes in the same parent
          const parentId = movingNode.parentNode;
          const siblingNodes = nodes.filter(n => 
            n.id !== change.id && 
            n.parentNode === parentId && 
            n.type === 'custom'
          );
          
          for (const sibling of siblingNodes) {
            const dx = Math.abs(newX - sibling.position.x);
            const dy = Math.abs(newY - sibling.position.y);
            
            // If nodes would overlap
            if (dx < nodeWidth + minDistance && dy < nodeHeight + minDistance) {
              // Push the moving node away
              if (dx < dy) {
                // Adjust Y position
                if (newY < sibling.position.y) {
                  newY = sibling.position.y - nodeHeight - minDistance;
                } else {
                  newY = sibling.position.y + nodeHeight + minDistance;
                }
              } else {
                // Adjust X position
                if (newX < sibling.position.x) {
                  newX = sibling.position.x - nodeWidth - minDistance;
                } else {
                  newX = sibling.position.x + nodeWidth + minDistance;
                }
              }
            }
          }
          
          return { ...change, position: { x: newX, y: newY } };
        }
        return change;
      });
      
      onNodesChange(modifiedChanges);
    } else {
      onNodesChange(changes);
    }
  }, [nodes, onNodesChange]);


  useEffect(() => {
    if (!data) return;

    // Filter out all pom projects
    const pomNodeIds = new Set(
      data.nodes
        .filter(node => node.packaging === 'pom')
        .map(node => node.id)
    );

    // Group nodes by repository for layout, excluding pom projects
    const nodesByRepo = new Map<string, Node[]>();
    data.nodes.forEach(node => {
      if (node.packaging !== 'pom') {
        const repo = node.nodeGroup;
        if (!nodesByRepo.has(repo)) {
          nodesByRepo.set(repo, []);
        }
        nodesByRepo.get(repo)!.push(node);
      }
    });

    // Calculate dependency levels for hierarchical layout
    const calculateNodeLevels = (nodes: Node[], links: any[]) => {
      const levels = new Map<string, number>();
      const inDegree = new Map<string, number>();
      const graph = new Map<string, string[]>();
      
      // Initialize
      nodes.forEach(node => {
        inDegree.set(node.id, 0);
        graph.set(node.id, []);
      });
      
      // Build graph and calculate in-degrees
      links.forEach(link => {
        const sourceId = typeof link.source === 'string' ? link.source : (link.source as any).id;
        const targetId = typeof link.target === 'string' ? link.target : (link.target as any).id;
        
        if (!pomNodeIds.has(sourceId) && !pomNodeIds.has(targetId)) {
          graph.get(sourceId)?.push(targetId);
          inDegree.set(targetId, (inDegree.get(targetId) || 0) + 1);
        }
      });
      
      // Topological sort with level assignment
      const queue: string[] = [];
      nodes.forEach(node => {
        if (inDegree.get(node.id) === 0) {
          queue.push(node.id);
          levels.set(node.id, 0);
        }
      });
      
      while (queue.length > 0) {
        const current = queue.shift()!;
        const currentLevel = levels.get(current) || 0;
        
        graph.get(current)?.forEach(neighbor => {
          const newInDegree = (inDegree.get(neighbor) || 0) - 1;
          inDegree.set(neighbor, newInDegree);
          
          if (newInDegree === 0) {
            queue.push(neighbor);
            levels.set(neighbor, currentLevel + 1);
          }
        });
      }
      
      // Assign levels to remaining nodes (in case of cycles)
      nodes.forEach(node => {
        if (!levels.has(node.id)) {
          levels.set(node.id, 0);
        }
      });
      
      return levels;
    };

    // Calculate grid layout for repositories (only non-empty ones)
    const repos = Array.from(nodesByRepo.entries()).filter(([, nodes]) => nodes.length > 0);
    const repoSpacing = 50; // Reduced spacing between repos
    const nodePadding = 30; // Reduced padding around nodes
    const nodeWidth = 150; // Fixed node width for calculations
    const nodeHeight = 80; // Fixed node height including text
    const nodeHorizontalSpacing = 40; // Reduced horizontal space between nodes
    const nodeVerticalSpacing = 60; // Reduced vertical space between levels

    // Calculate levels for all nodes
    const allNodes = repos.flatMap(([, nodes]) => nodes);
    const nodeLevels = calculateNodeLevels(allNodes, data.links);

    // First pass: calculate all repository dimensions with hierarchical layout
    const repoDimensions = new Map<string, { width: number; height: number; nodeLayout: Map<string, {x: number, y: number}> }>();
    
    repos.forEach(([repoName, repoNodes]) => {
      // Group nodes by level within this repository
      const levelGroups = new Map<number, Node[]>();
      let maxLevel = 0;
      
      repoNodes.forEach(node => {
        const level = nodeLevels.get(node.id) || 0;
        maxLevel = Math.max(maxLevel, level);
        if (!levelGroups.has(level)) {
          levelGroups.set(level, []);
        }
        levelGroups.get(level)!.push(node);
      });
      
      // Calculate positions for nodes in each level
      const nodeLayout = new Map<string, {x: number, y: number}>();
      let maxWidth = 0;
      
      for (let level = 0; level <= maxLevel; level++) {
        const nodesAtLevel = levelGroups.get(level) || [];
        const levelWidth = nodesAtLevel.length * nodeWidth + (nodesAtLevel.length - 1) * nodeHorizontalSpacing;
        maxWidth = Math.max(maxWidth, levelWidth);
        
        // Center nodes at this level
        const startX = nodePadding + (maxWidth - levelWidth) / 2;
        
        nodesAtLevel.forEach((node, index) => {
          const x = startX + index * (nodeWidth + nodeHorizontalSpacing);
          const y = nodePadding + 30 + level * (nodeHeight + nodeVerticalSpacing); // +30 for title space
          nodeLayout.set(node.id, { x, y });
        });
      }
      
      // Calculate repository dimensions
      const repoWidth = Math.max(
        maxWidth + nodePadding * 2,
        repoName.length * 18 * 0.7 * 1.1 + nodePadding * 2 // Text width estimation
      );
      const repoHeight = nodePadding * 2 + 30 + (maxLevel + 1) * nodeHeight + maxLevel * nodeVerticalSpacing;
      
      repoDimensions.set(repoName, { width: repoWidth, height: repoHeight, nodeLayout });
    });

    // Second pass: calculate flexible grid layout
    const flowNodes: FlowNode[] = [];
    const nodePositions = new Map<string, { x: number; y: number }>();
    
    // Sort repositories by total dependency count (repos with more dependencies first)
    const repoDepCount = new Map<string, number>();
    repos.forEach(([repoName, repoNodes]) => {
      let depCount = 0;
      repoNodes.forEach(node => {
        data.links.forEach(link => {
          const sourceId = typeof link.source === 'string' ? link.source : (link.source as any).id;
          const targetId = typeof link.target === 'string' ? link.target : (link.target as any).id;
          if (sourceId === node.id || targetId === node.id) {
            depCount++;
          }
        });
      });
      repoDepCount.set(repoName, depCount);
    });
    
    const sortedRepos = repos.sort((a, b) => (repoDepCount.get(b[0]) || 0) - (repoDepCount.get(a[0]) || 0));
    const maxWidth = 1800; // Reduced maximum viewport width for more compact layout
    
    let currentX = 0;
    let currentY = 0;
    let rowHeight = 0;
    
    sortedRepos.forEach(([repoName, repoNodes]) => {
      const dimensions = repoDimensions.get(repoName)!;
      
      // Check if we need to wrap to next row
      if (currentX > 0 && currentX + dimensions.width > maxWidth) {
        currentX = 0;
        currentY += rowHeight + repoSpacing;
        rowHeight = 0;
      }
      
      // Update row height
      rowHeight = Math.max(rowHeight, dimensions.height);

      // Add repository group node
      flowNodes.push({
        id: `group-${repoName}`,
        type: 'group',
        position: { x: currentX, y: currentY },
        data: { 
          label: repoName,
          color: '#6b7280',
        },
        style: {
          width: dimensions.width,
          height: dimensions.height,
          backgroundColor: 'rgba(243, 244, 246, 0.5)',
          border: '2px solid #9ca3af',
          borderRadius: '16px',
          boxShadow: '0 8px 16px rgba(0, 0, 0, 0.08)',
        },
        draggable: false,
        connectable: false,
        selectable: false,
      });

      // Position nodes within repository using hierarchical layout
      repoNodes.forEach((node) => {
        const pos = dimensions.nodeLayout.get(node.id)!;

        // Store absolute position for edge routing
        nodePositions.set(node.id, { x: currentX + pos.x, y: currentY + pos.y });

        flowNodes.push({
          id: node.id,
          type: 'custom',
          position: { x: pos.x, y: pos.y }, // Relative position within parent
          data: {
            label: node.name,
            version: node.version,
            packaging: node.packaging,
            selected: selectedNode === node.id,
          },
          parentNode: `group-${repoName}`,
          extent: 'parent',
          draggable: true,
          connectable: false,
          selectable: true,
          zIndex: 10,
        });
      });
      
      // Move to next position
      currentX += dimensions.width + repoSpacing;
    });

    // Create React Flow edges, excluding those connected to pom projects
    const flowEdges: Edge[] = data.links
      .filter(link => {
        const sourceId = typeof link.source === 'string' ? link.source : (link.source as any).id;
        const targetId = typeof link.target === 'string' ? link.target : (link.target as any).id;
        return !pomNodeIds.has(sourceId) && !pomNodeIds.has(targetId);
      })
      .map((link, index) => {
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
            width: 15,
            height: 15,
          },
        };
      });

    setNodes(flowNodes);
    setEdges(flowEdges);
  }, [data, selectedNode, selectedRepository, setNodes, setEdges]);

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
            width: 15,
            height: 15,
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
        onNodesChange={handleNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ 
          padding: 0.15,
          includeHiddenNodes: false,
          minZoom: 0.3,
          maxZoom: 1.5,
          duration: 800
        }}
        minZoom={0.2}
        maxZoom={3}
        defaultViewport={{ x: 50, y: 50, zoom: 0.8 }}
        nodesDraggable={true}
        nodesConnectable={false}
        elementsSelectable={true}
        preventScrolling={false}
        snapToGrid={true}
        snapGrid={[10, 10]}
        zoomOnScroll={false}
        zoomOnPinch={true}
        zoomOnDoubleClick={true}
        panOnScroll={false}
        panOnScrollSpeed={0.5}
        panOnDrag={true}
        zoomActivationKeyCode={['Control', 'Meta']}
      >
        <Background variant={BackgroundVariant.Dots} color="#e5e7eb" gap={20} size={1} />
        <CustomZoomControls />
        <MiniMap 
          nodeColor={(node) => {
            if (node.type === 'group') {
              return 'rgba(240, 240, 240, 0.5)';
            }
            return '#6b7280';
          }}
          style={{
            backgroundColor: 'rgba(255, 255, 255, 0.8)',
          }}
        />
      </ReactFlow>
    </div>
  );
};


// Wrapper with ReactFlowProvider for zoom control
const GraphWithProvider: React.FC<GraphProps> = (props) => {
  return (
    <ReactFlowProvider>
      <div style={{ width: '100%', height: '100%' }}>
        <GraphContent {...props} />
      </div>
    </ReactFlowProvider>
  );
};

export const Graph = GraphWithProvider;