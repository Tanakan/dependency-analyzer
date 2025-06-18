#!/bin/bash

echo "=== Auto-centering Test Script ==="
echo ""
echo "1. Starting Spring Boot application..."
echo "   Please run 'mvn spring-boot:run' in another terminal"
echo ""
echo "2. Once the application is running, open Chrome and navigate to:"
echo "   http://localhost:8080"
echo ""
echo "3. Open Chrome DevTools (F12) and go to Console tab"
echo ""
echo "4. Run these tests in the browser console:"
echo ""

cat << 'EOF'
// Test 1: Filter by a single project
console.log("=== Test 1: Single Project Filter ===");
filterByProject('com.example:user-service');

// Wait 2 seconds then check
setTimeout(() => {
    console.log("Check: The view should be centered on user-service and its dependencies");
    
    // Test 2: Filter by another project
    console.log("\n=== Test 2: Another Project Filter ===");
    filterByProject('com.example:common-libs');
}, 2000);

// Test 3: Filter by repository
setTimeout(() => {
    console.log("\n=== Test 3: Repository Filter ===");
    filterByRepository('ecommerce-platform');
}, 4000);

// Test 4: Clear filter and test WAR project
setTimeout(() => {
    console.log("\n=== Test 4: WAR Project Filter ===");
    clearFilter();
    setTimeout(() => {
        filterByProject('com.example:admin-portal');
    }, 1000);
}, 6000);

// Test 5: Check transform values
setTimeout(() => {
    console.log("\n=== Test 5: Current Transform ===");
    const currentTransform = d3.zoomTransform(svg.node());
    console.log("Current transform:", {
        x: currentTransform.x,
        y: currentTransform.y,
        scale: currentTransform.k
    });
}, 8000);
EOF

echo ""
echo "5. Expected behavior:"
echo "   - When filtering, the view should animate to center on the filtered nodes"
echo "   - The zoom level should adjust to fit all filtered nodes"
echo "   - Console should show detailed debug information"
echo ""
echo "6. If centering doesn't work, check console for errors and debug output"