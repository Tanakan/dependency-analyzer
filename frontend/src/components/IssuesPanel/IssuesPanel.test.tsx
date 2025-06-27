import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import { IssuesPanel } from './IssuesPanel';
import { IssuesData } from '../../types';

describe('IssuesPanel Component', () => {
  const mockIssues: IssuesData = {
    circularReferences: [
      ['com.example:service-a:1.0.0', 'com.example:service-b:1.0.0', 'com.example:service-a:1.0.0'],
      ['com.example:service-c:1.0.0', 'com.example:service-d:1.0.0', 'com.example:service-c:1.0.0']
    ],
    unreferencedProjects: [
      'com.example:orphan-project-1',
      'com.example:orphan-project-2'
    ],
    duplicateArtifactIds: {
      'config-service': ['com.example.config:config-service#1', 'com.example.config:config-service'],
      'common-lib': ['com.example:common-lib#1', 'com.example:common-lib#2']
    },
    duplicateGAVs: {
      'com.example.config:config-service:1.0.0': ['com.example.config:config-service#1', 'com.example.config:config-service']
    }
  };

  it('renders without issues when no data is provided', () => {
    render(<IssuesPanel issues={undefined} />);
    expect(screen.queryByText('Project Issues Analysis')).not.toBeInTheDocument();
  });

  it('displays the issues panel header', () => {
    render(<IssuesPanel issues={mockIssues} />);
    expect(screen.getByText('Project Issues Analysis')).toBeInTheDocument();
  });

  it('shows correct issue counts in summary', () => {
    render(<IssuesPanel issues={mockIssues} />);
    
    // Check summary labels exist
    expect(screen.getByText('Circular References:')).toBeInTheDocument();
    expect(screen.getByText('Unreferenced Projects:')).toBeInTheDocument();
    expect(screen.getByText('Duplicate Artifact IDs:')).toBeInTheDocument();
    expect(screen.getByText('Duplicate GAVs:')).toBeInTheDocument();
    
    // Verify the summary counts have correct values and classes
    const circularCount = screen.getByTestId('summary-count-circular');
    expect(circularCount).toHaveTextContent('2');
    expect(circularCount).toHaveClass('error');
    
    const unreferencedCount = screen.getByTestId('summary-count-unreferenced');
    expect(unreferencedCount).toHaveTextContent('2');
    expect(unreferencedCount).toHaveClass('warning');
    
    const duplicateArtifactsCount = screen.getByTestId('summary-count-duplicate-artifacts');
    expect(duplicateArtifactsCount).toHaveTextContent('2');
    expect(duplicateArtifactsCount).toHaveClass('info');
    
    const duplicateGAVsCount = screen.getByTestId('summary-count-duplicate-gavs');
    expect(duplicateGAVsCount).toHaveTextContent('1');
    expect(duplicateGAVsCount).toHaveClass('error');
  });

  it('expands and collapses sections when clicked', () => {
    render(<IssuesPanel issues={mockIssues} />);
    
    // Find the Unreferenced Projects section (which starts collapsed)
    const unreferencedSection = screen.getByText('Unreferenced Projects (2)').closest('.section-header');
    
    // Initially collapsed - projects should not be visible
    expect(screen.queryByText('com.example:orphan-project-1')).not.toBeInTheDocument();
    
    // Click to expand
    fireEvent.click(unreferencedSection!);
    expect(screen.getByText('com.example:orphan-project-1')).toBeInTheDocument();
    expect(screen.getByText('com.example:orphan-project-2')).toBeInTheDocument();
    
    // Click to collapse
    fireEvent.click(unreferencedSection!);
    expect(screen.queryByText('com.example:orphan-project-1')).not.toBeInTheDocument();
  });

  it('displays circular references correctly', () => {
    render(<IssuesPanel issues={mockIssues} />);
    
    // Circular references section is expanded by default
    expect(screen.getByText('Cycle 1:')).toBeInTheDocument();
    expect(screen.getByText('com.example:service-a:1.0.0 → com.example:service-b:1.0.0 → com.example:service-a:1.0.0')).toBeInTheDocument();
    
    expect(screen.getByText('Cycle 2:')).toBeInTheDocument();
    expect(screen.getByText('com.example:service-c:1.0.0 → com.example:service-d:1.0.0 → com.example:service-c:1.0.0')).toBeInTheDocument();
  });

  it('displays duplicate artifact IDs correctly when expanded', () => {
    render(<IssuesPanel issues={mockIssues} />);
    
    // Click to expand duplicate artifact IDs section
    const duplicateArtifactsSection = screen.getByText('Duplicate Artifact IDs (2)').closest('.section-header');
    fireEvent.click(duplicateArtifactsSection!);
    
    // Check if duplicate groups are displayed
    expect(screen.getByText('config-service')).toBeInTheDocument();
    expect(screen.getByText('com.example.config:config-service#1')).toBeInTheDocument();
    expect(screen.getByText('com.example.config:config-service')).toBeInTheDocument();
    
    expect(screen.getByText('common-lib')).toBeInTheDocument();
    expect(screen.getByText('com.example:common-lib#1')).toBeInTheDocument();
    expect(screen.getByText('com.example:common-lib#2')).toBeInTheDocument();
  });

  it('displays no issues message when section is empty', () => {
    const emptyIssues: IssuesData = {
      circularReferences: [],
      unreferencedProjects: [],
      duplicateArtifactIds: {},
      duplicateGAVs: {}
    };
    
    render(<IssuesPanel issues={emptyIssues} />);
    
    // Circular references section is expanded by default
    expect(screen.getByText('No circular references found')).toBeInTheDocument();
  });

  it('applies correct CSS classes for issue severity', () => {
    render(<IssuesPanel issues={mockIssues} />);
    
    // Check error class for circular references count
    const circularCount = screen.getByTestId('summary-count-circular');
    expect(circularCount).toHaveClass('summary-count', 'error');
    
    // Check warning class for unreferenced projects count
    const unreferencedCount = screen.getByTestId('summary-count-unreferenced');
    expect(unreferencedCount).toHaveClass('summary-count', 'warning');
    
    // Check info class for duplicate artifact IDs count
    const duplicateArtifactCount = screen.getByTestId('summary-count-duplicate-artifacts');
    expect(duplicateArtifactCount).toHaveClass('summary-count', 'info');
    
    // Check error class for duplicate GAVs count
    const duplicateGAVsCount = screen.getByTestId('summary-count-duplicate-gavs');
    expect(duplicateGAVsCount).toHaveClass('summary-count', 'error');
  });
});