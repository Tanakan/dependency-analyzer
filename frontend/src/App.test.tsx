import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';

// Mock D3.js to avoid import issues in tests
jest.mock('d3', () => ({
  select: jest.fn(),
  forceSimulation: jest.fn(),
  forceLink: jest.fn(),
  forceManyBody: jest.fn(),
  forceCollide: jest.fn(),
  drag: jest.fn(),
  scaleOrdinal: jest.fn(() => jest.fn()),
  schemeCategory10: [],
  color: jest.fn(),
  zoom: jest.fn(() => ({
    scaleExtent: jest.fn(() => ({ on: jest.fn() }))
  }))
}));

test('renders dependencies analyzer heading', () => {
  render(<App />);
  const headingElement = screen.getByRole('heading', { level: 1 });
  expect(headingElement).toHaveTextContent('Dependencies Analyzer');
});

test('renders welcome message when no data is loaded', () => {
  render(<App />);
  const welcomeElement = screen.getByText(/Welcome to Dependencies Analyzer/i);
  expect(welcomeElement).toBeInTheDocument();
});