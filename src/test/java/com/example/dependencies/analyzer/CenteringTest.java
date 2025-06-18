package com.example.dependencies.analyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class CenteringTest {
    
    @LocalServerPort
    private int port;
    
    private final TestRestTemplate restTemplate = new TestRestTemplate();
    
    @Test
    void testIndexPageHasCenteringElements() {
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        String html = response.getBody();
        
        // Verify centering function exists
        assertTrue(html.contains("centerOnFilteredNodes"), 
            "centerOnFilteredNodes function should be present");
        
        // Verify centering indicator exists
        assertTrue(html.contains("centering-indicator"), 
            "Centering indicator div should be present");
        assertTrue(html.contains("フィルタした箇所に移動中"), 
            "Centering indicator text should be present");
        
        // Verify transform calculations
        assertTrue(html.contains("d3.zoomIdentity"), 
            "D3 zoom identity should be used");
        assertTrue(html.contains(".translate(svgWidth / 2, svgHeight / 2)"), 
            "Center translation should be present");
        
        // Verify simulation restart logic
        assertTrue(html.contains("simulation.alpha"), 
            "Simulation alpha check should be present");
        
        // Verify debug logging
        assertTrue(html.contains("console.log('Centering on nodes:'"), 
            "Debug logging should be present");
        assertTrue(html.contains("console.log('Centering animation completed')"), 
            "Animation completion logging should be present");
        
        // Print summary
        System.out.println("Centering elements verified:");
        System.out.println("- centerOnFilteredNodes function: ✓");
        System.out.println("- Centering indicator: ✓");
        System.out.println("- Transform calculations: ✓");
        System.out.println("- Simulation restart: ✓");
        System.out.println("- Debug logging: ✓");
    }
}