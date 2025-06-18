# Zoom Improvements for Filtered Nodes

## Changes Made:

### 1. **Increased Zoom Scale Limits**
- Maximum zoom scale increased from 10 to 20
- Allows much closer viewing of nodes

### 2. **Reduced Padding Around Filtered Nodes**
- Single node: padding reduced from 100 to 50 pixels
- Small groups (≤5 nodes): padding reduced from 80 to 40 pixels
- Other groups: padding reduced from 50 to 30 pixels

### 3. **Higher Zoom Levels for Different Node Counts**
- **Single node**: 3.0x - 5.0x zoom (was 1.5x - 3.0x)
- **2-3 nodes**: 2.5x - 4.0x zoom (was 1.2x - 2.5x)
- **4-10 nodes**: 1.5x - 3.0x zoom (was 0.8x - 2.0x)
- **11-20 nodes**: 1.0x - 2.5x zoom (new category)
- **Many nodes (>20)**: 0.8x - 2.0x zoom (was 0.5x - 1.5x)

### 4. **More Compact Node Layout**
- Link distance reduced from 150 to 100 pixels
- Force charge strength reduced from -500 to -300
- Collision radius reduced from 30 to 25 pixels
- Results in a more compact graph layout

### 5. **Improved Zoom Calculation**
- Scale factor increased from 0.85 to 0.95 for better space utilization
- Nodes fill more of the available viewport when filtered

## Testing the Changes:

To test the zoom functionality:

1. Open the browser console on the dependency graph page
2. Run: `testFilter()` to test various filter scenarios
3. Run: `debugZoomState()` to check current zoom state

Or manually test by:
- Clicking on a single node to see it zoom very close
- Clicking on a repository with few projects to see closer zoom
- Clicking on repositories with many projects to see appropriate zoom levels

## Result:

The filtered nodes now appear much closer and larger in the viewport, making it easier to see details and relationships between nodes.