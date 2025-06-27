export interface Node {
  id: string;
  name: string;
  version: string;
  group: string;
  type: 'Maven' | 'Gradle';
  packaging: 'jar' | 'war' | 'pom';
  nodeGroup: string;
  x?: number;
  y?: number;
  fx?: number;
  fy?: number;
}

export interface Link {
  source: string | Node;
  target: string | Node;
  value: number;
}

export interface GraphData {
  nodes: Node[];
  links: Link[];
  stats?: {
    totalProjects: number;
    totalDependencies: number;
  };
  analysisDate?: string;
  version?: string;
}

export interface Repository {
  name: string;
  nodes: Node[];
  projectCount: number;
}