import React, { useEffect, useRef, useState } from 'react';
import * as d3 from 'd3';
import { GraphData, Node, Link } from '../../types';
import './Graph.css';

interface GraphProps {
  data: GraphData | null;
  selectedNode: string | null;
  selectedRepository: string | null;
}

export const Graph: React.FC<GraphProps> = ({ data, selectedNode, selectedRepository }) => {
  const svgRef = useRef<SVGSVGElement>(null);
  const [dimensions, setDimensions] = useState({ width: 800, height: 600 });

  useEffect(() => {
    const updateDimensions = () => {
      const container = svgRef.current?.parentElement;
      if (container) {
        setDimensions({
          width: container.clientWidth,
          height: container.clientHeight
        });
      }
    };

    updateDimensions();
    window.addEventListener('resize', updateDimensions);
    return () => window.removeEventListener('resize', updateDimensions);
  }, []);

  useEffect(() => {
    if (!data || !svgRef.current) return;

    // Clear previous graph
    d3.select(svgRef.current).selectAll('*').remove();

    const svg = d3.select(svgRef.current);
    const { width, height } = dimensions;

    // Create arrow marker
    const defs = svg.append('defs');
    const marker = defs.append('marker')
      .attr('id', 'arrowhead')
      .attr('viewBox', '0 -5 10 10')
      .attr('refX', 25)
      .attr('refY', 0)
      .attr('orient', 'auto')
      .attr('markerWidth', 8)
      .attr('markerHeight', 8)
      .attr('markerUnits', 'userSpaceOnUse');

    marker.append('path')
      .attr('d', 'M 0,-5 L 10 ,0 L 0,5')
      .attr('fill', '#999');

    // Create zoom behavior
    const zoom = d3.zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.1, 10])
      .on('zoom', (event) => {
        g.attr('transform', event.transform);
      });

    svg.call(zoom);

    // Create main group
    const g = svg.append('g');

    // Color scale
    const color = d3.scaleOrdinal(d3.schemeCategory10);

    // Group nodes by repository
    const nodesByRepo = d3.group(data.nodes, d => d.nodeGroup);

    // Calculate repository layouts
    const repoLayouts = calculateRepositoryLayouts(nodesByRepo, width, height);

    // Draw repository frames
    const repoFrames = g.append('g')
      .attr('class', 'repo-frames')
      .selectAll('g')
      .data(Array.from(repoLayouts.entries()))
      .enter()
      .append('g');

    repoFrames.append('rect')
      .attr('x', d => d[1].x)
      .attr('y', d => d[1].y)
      .attr('width', d => d[1].width)
      .attr('height', d => d[1].height)
      .attr('rx', 10)
      .attr('fill', '#f5f5f5')
      .attr('stroke', '#666')
      .attr('stroke-width', 2)
      .attr('stroke-dasharray', d => d[1].nodes.length > 1 ? '10,5' : 'none');

    repoFrames.append('text')
      .attr('x', d => d[1].x + 15)
      .attr('y', d => d[1].y + 25)
      .text(d => d[0])
      .style('font-weight', '600')
      .style('font-size', '14px')
      .style('fill', '#667eea')
      .style('text-shadow', '0 1px 2px rgba(255,255,255,0.8)');

    // Create links
    const link = g.append('g')
      .selectAll('path')
      .data(data.links)
      .enter()
      .append('path')
      .attr('class', 'link')
      .attr('stroke', '#999')
      .attr('stroke-width', 2)
      .attr('fill', 'none')
      .attr('marker-end', 'url(#arrowhead)');

    // Create nodes
    const node = g.append('g')
      .selectAll('g')
      .data(data.nodes)
      .enter()
      .append('g')
      .attr('class', 'node')
      .call(d3.drag<SVGGElement, Node>()
        .on('start', dragstarted)
        .on('drag', dragged)
        .on('end', dragended));

    // Add circles/rectangles for nodes with enhanced styling
    node.each(function(d) {
      const nodeElement = d3.select(this);
      
      if (d.packaging === 'war') {
        // Add gradient for WAR packages
        const gradientId = `gradient-war-${d.id.replace(/[^a-zA-Z0-9]/g, '')}`;
        defs.append('linearGradient')
          .attr('id', gradientId)
          .selectAll('stop')
          .data([
            { offset: '0%', color: '#ff6b6b' },
            { offset: '100%', color: '#ff8e8e' }
          ])
          .enter()
          .append('stop')
          .attr('offset', d => d.offset)
          .attr('stop-color', d => d.color);
          
        nodeElement.append('rect')
          .attr('x', -12)
          .attr('y', -12)
          .attr('width', 24)
          .attr('height', 24)
          .attr('rx', 4)
          .attr('fill', `url(#${gradientId})`)
          .attr('stroke', '#ff4757')
          .attr('stroke-width', 2);
      } else {
        // Add gradient for JAR packages
        const gradientId = `gradient-jar-${d.id.replace(/[^a-zA-Z0-9]/g, '')}`;
        defs.append('radialGradient')
          .attr('id', gradientId)
          .selectAll('stop')
          .data([
            { offset: '0%', color: color(d.nodeGroup) },
            { offset: '100%', color: d3.color(color(d.nodeGroup))?.darker(0.3)?.toString() || color(d.nodeGroup) }
          ])
          .enter()
          .append('stop')
          .attr('offset', d => d.offset)
          .attr('stop-color', d => d.color);
          
        nodeElement.append('circle')
          .attr('r', 12)
          .attr('fill', `url(#${gradientId})`)
          .attr('stroke', '#667eea')
          .attr('stroke-width', 2);
      }
    });

    // Add labels with enhanced styling
    node.append('text')
      .attr('dy', 28)
      .attr('text-anchor', 'middle')
      .style('font-size', '11px')
      .style('font-weight', '500')
      .style('fill', '#333')
      .style('text-shadow', '0 1px 2px rgba(255,255,255,0.8)')
      .text(d => d.name);

    // Position nodes within repositories
    positionNodesInRepositories(data.nodes, repoLayouts);

    // Create simulation
    const simulation = d3.forceSimulation(data.nodes)
      .force('link', d3.forceLink<Node, Link>(data.links)
        .id(d => d.id)
        .distance(100))
      .force('charge', d3.forceManyBody().strength(-300))
      .force('collision', d3.forceCollide().radius(30))
      .stop();

    // Update positions
    node.attr('transform', d => `translate(${d.x},${d.y})`);
    
    updateLinks();

    // Drag functions
    function dragstarted(event: d3.D3DragEvent<SVGGElement, Node, Node>, d: Node) {
      d.fx = d.x;
      d.fy = d.y;
    }

    function dragged(event: d3.D3DragEvent<SVGGElement, Node, Node>, d: Node) {
      d.fx = event.x;
      d.fy = event.y;
      d.x = event.x;
      d.y = event.y;
      
      d3.select(event.sourceEvent.target.parentNode)
        .attr('transform', `translate(${d.x},${d.y})`);
      
      updateLinks();
    }

    function dragended(event: d3.D3DragEvent<SVGGElement, Node, Node>, d: Node) {
      d.fx = d.x;
      d.fy = d.y;
      
      // Fix disappearing arrows
      link.each(function() {
        const linkElement = d3.select(this);
        linkElement.style('display', 'none');
        // Force reflow by triggering layout calculation
        const node = linkElement.node();
        if (node) {
          // Use getBoundingClientRect for SVG elements
          void node.getBoundingClientRect();
        }
        linkElement.style('display', null);
        linkElement.attr('marker-end', 'url(#arrowhead)');
      });
    }

    function updateLinks() {
      if (!data) return;
      
      link.attr('d', d => {
        const source = typeof d.source === 'object' ? d.source : data.nodes.find(n => n.id === d.source)!;
        const target = typeof d.target === 'object' ? d.target : data.nodes.find(n => n.id === d.target)!;
        
        const dx = target.x! - source.x!;
        const dy = target.y! - source.y!;
        const dr = Math.sqrt(dx * dx + dy * dy);
        
        return `M${source.x},${source.y} A${dr},${dr} 0 0,1 ${target.x},${target.y}`;
      });
    }

  }, [data, dimensions]);

  useEffect(() => {
    if (!svgRef.current) return;

    const svg = d3.select(svgRef.current);
    
    // Handle node selection
    svg.selectAll('.node')
      .classed('highlighted', d => (d as Node).id === selectedNode)
      .classed('dimmed', d => selectedNode !== null && (d as Node).id !== selectedNode);

    // Handle repository selection
    svg.selectAll('.node')
      .classed('repo-highlighted', d => (d as Node).nodeGroup === selectedRepository)
      .classed('repo-dimmed', d => selectedRepository !== null && (d as Node).nodeGroup !== selectedRepository);

  }, [selectedNode, selectedRepository]);

  return (
    <div className="graph-container">
      <svg ref={svgRef} width={dimensions.width} height={dimensions.height} />
    </div>
  );
};

function calculateRepositoryLayouts(
  nodesByRepo: d3.InternMap<string, Node[]>,
  width: number,
  height: number
): Map<string, { x: number; y: number; width: number; height: number; nodes: Node[] }> {
  const layouts = new Map<string, { x: number; y: number; width: number; height: number; nodes: Node[] }>();
  
  const repos = Array.from(nodesByRepo.entries());
  const cols = Math.ceil(Math.sqrt(repos.length));
  const rows = Math.ceil(repos.length / cols);
  
  const cellWidth = width / cols;
  const cellHeight = height / rows;
  const padding = 40;

  repos.forEach(([repoName, nodes], index) => {
    const col = index % cols;
    const row = Math.floor(index / cols);
    
    layouts.set(repoName, {
      x: col * cellWidth + padding,
      y: row * cellHeight + padding,
      width: cellWidth - 2 * padding,
      height: cellHeight - 2 * padding,
      nodes
    });
  });

  return layouts;
}

function positionNodesInRepositories(
  nodes: Node[],
  repoLayouts: Map<string, { x: number; y: number; width: number; height: number; nodes: Node[] }>
) {
  repoLayouts.forEach((layout, repoName) => {
    const repoNodes = nodes.filter(n => n.nodeGroup === repoName);
    const nodeSpacing = 100;
    
    if (repoNodes.length === 1) {
      repoNodes[0].x = layout.x + layout.width / 2;
      repoNodes[0].y = layout.y + layout.height / 2;
    } else {
      const cols = Math.ceil(Math.sqrt(repoNodes.length));
      const rows = Math.ceil(repoNodes.length / cols);
      
      repoNodes.forEach((node, i) => {
        const col = i % cols;
        const row = Math.floor(i / cols);
        node.x = layout.x + (col + 0.5) * (layout.width / cols);
        node.y = layout.y + (row + 0.5) * (layout.height / rows);
      });
    }
  });
}