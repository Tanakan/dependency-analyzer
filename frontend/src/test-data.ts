// Test data to verify issues panel works correctly
export const testDataWithIssues = {
  nodes: [
    {
      id: "com.example:service-a",
      name: "service-a",
      version: "1.0.0",
      group: "com.example",
      type: "Maven" as const,
      packaging: "jar" as const,
      nodeGroup: "circular-deps-example"
    },
    {
      id: "com.example:service-b",
      name: "service-b",
      version: "1.0.0",
      group: "com.example",
      type: "Maven" as const,
      packaging: "jar" as const,
      nodeGroup: "circular-deps-example"
    }
  ],
  links: [
    {
      source: "com.example:service-a",
      target: "com.example:service-b",
      value: 1
    }
  ],
  stats: {
    totalProjects: 2,
    totalDependencies: 1
  },
  issues: {
    circularReferences: [
      ["com.example:service-a:1.0.0", "com.example:service-b:1.0.0", "com.example:service-a:1.0.0"]
    ],
    unreferencedProjects: ["com.example:unreferenced-project"],
    duplicateArtifactIds: {
      "config-service": ["com.example.config:config-service#1", "com.example.config:config-service"]
    },
    duplicateGAVs: {
      "com.example.config:config-service:1.0.0": ["com.example.config:config-service#1", "com.example.config:config-service"]
    }
  }
};