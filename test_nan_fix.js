// Test script to verify NaN fixes in browser console
const testData = {
    "nodes": [
        {"id": "test1", "name": "test1", "x": 100, "y": 100},
        {"id": "test2", "name": "test2", "x": 100, "y": 100}, // Same position to test division by zero
        {"id": "test3", "name": "test3", "x": 200, "y": 150}
    ],
    "links": [
        {"source": 0, "target": 1, "value": 1}
    ]
};

// Test for NaN in distance calculation
function testDistanceCalculation() {
    const sourceNode = testData.nodes[0];
    const targetNode = testData.nodes[1];
    
    const dx = targetNode.x - sourceNode.x;
    const dy = targetNode.y - sourceNode.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    console.log("Distance calculation test:");
    console.log("dx:", dx, "dy:", dy, "distance:", distance);
    
    // Test division by zero fix
    const offset = 10;
    const midX = distance > 0 ? (sourceNode.x + targetNode.x) / 2 + offset * dy / distance : (sourceNode.x + targetNode.x) / 2;
    const midY = distance > 0 ? (sourceNode.y + targetNode.y) / 2 - offset * dx / distance : (sourceNode.y + targetNode.y) / 2;
    
    console.log("midX:", midX, "midY:", midY);
    console.log("NaN check - midX isNaN:", isNaN(midX), "midY isNaN:", isNaN(midY));
}

// Test for angle calculation with different node counts
function testAngleCalculation() {
    console.log("Angle calculation tests:");
    
    // Test with 0 nodes
    const angleStep0 = 0 > 0 ? (2 * Math.PI) / 0 : 0;
    console.log("0 nodes - angleStep:", angleStep0, "isNaN:", isNaN(angleStep0));
    
    // Test with 1 node
    const angleStep1 = 1 > 1 ? (2 * Math.PI) / (1 - 1) : 0;
    console.log("1 node - angleStep:", angleStep1, "isNaN:", isNaN(angleStep1));
    
    // Test with 2 nodes
    const angleStep2 = 2 > 1 ? (2 * Math.PI) / (2 - 1) : 0;
    console.log("2 nodes - angleStep:", angleStep2, "isNaN:", isNaN(angleStep2));
}

// Test transform function with null/undefined data
function testTransformFunction() {
    console.log("Transform function tests:");
    
    function safeTransform(d) {
        if (!d || isNaN(d.x) || isNaN(d.y)) {
            console.error("Invalid node data in transform:", d);
            return `translate(0,0)`;
        }
        return `translate(${d.x},${d.y})`;
    }
    
    // Test with valid data
    const result1 = safeTransform({x: 100, y: 200});
    console.log("Valid data result:", result1);
    
    // Test with null data
    const result2 = safeTransform(null);
    console.log("Null data result:", result2);
    
    // Test with NaN coordinates
    const result3 = safeTransform({x: NaN, y: 100});
    console.log("NaN coordinate result:", result3);
    
    // Test with undefined data
    const result4 = safeTransform(undefined);
    console.log("Undefined data result:", result4);
}

console.log("Running NaN fix tests...");
testDistanceCalculation();
testAngleCalculation();
testTransformFunction();
console.log("NaN fix tests completed.");