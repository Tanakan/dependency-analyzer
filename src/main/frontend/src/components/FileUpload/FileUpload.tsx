import React, { useRef } from 'react';
import { GraphData } from '../../types';
import './FileUpload.css';

interface FileUploadProps {
  onDataLoaded: (data: GraphData) => void;
}

export const FileUpload: React.FC<FileUploadProps> = ({ onDataLoaded }) => {
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileUpload = (event: React.ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = (e) => {
      try {
        const data = JSON.parse(e.target?.result as string) as GraphData;
        onDataLoaded(data);
      } catch (error) {
        console.error('Failed to parse JSON:', error);
        alert('Failed to parse JSON file. Please check the file format.');
      }
    };
    reader.readAsText(file);
  };

  const handleLoadDefault = async () => {
    try {
      const response = await fetch('/api/dependencies/data');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      onDataLoaded(data);
    } catch (error) {
      console.error('Failed to load default file:', error);
      // Fallback to static file
      try {
        const fallbackResponse = await fetch('/react/dependencies-analysis.json');
        if (fallbackResponse.ok) {
          const fallbackData = await fallbackResponse.json();
          onDataLoaded(fallbackData);
        } else {
          alert('Failed to load analysis data. Please upload a JSON file.');
        }
      } catch (fallbackError) {
        console.error('Fallback also failed:', fallbackError);
        alert('Failed to load analysis data. Please upload a JSON file.');
      }
    }
  };

  return (
    <div className="file-upload">
      <input
        ref={fileInputRef}
        type="file"
        accept=".json"
        onChange={handleFileUpload}
        style={{ display: 'none' }}
      />
      <button onClick={() => fileInputRef.current?.click()}>
        Upload JSON File
      </button>
      <button onClick={handleLoadDefault}>
        Load Default Analysis
      </button>
    </div>
  );
};