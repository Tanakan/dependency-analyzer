import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import GraphPage from './pages/GraphPage';
import IssuesPage from './pages/IssuesPage';
import './App.css';

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<GraphPage />} />
        <Route path="/issues" element={<IssuesPage />} />
      </Routes>
    </Router>
  );
}

export default App;