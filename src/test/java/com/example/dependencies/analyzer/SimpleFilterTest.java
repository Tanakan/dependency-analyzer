package com.example.dependencies.analyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SimpleFilterTest {
    
    @LocalServerPort
    private int port;
    
    private final TestRestTemplate restTemplate = new TestRestTemplate();
    
    @Test
    void testDataEndpoint() {
        String url = "http://localhost:" + port + "/api/dependencies/data";
        ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        Map<String, Object> data = response.getBody();
        
        // Verify data structure
        assertTrue(data.containsKey("nodes"));
        assertTrue(data.containsKey("links"));
        assertTrue(data.containsKey("stats"));
        
        // Verify nodes have required fields
        List<Map<String, Object>> nodes = (List<Map<String, Object>>) data.get("nodes");
        assertFalse(nodes.isEmpty());
        
        Map<String, Object> firstNode = nodes.get(0);
        assertTrue(firstNode.containsKey("id"));
        assertTrue(firstNode.containsKey("name"));
        assertTrue(firstNode.containsKey("packaging"));
        assertTrue(firstNode.containsKey("nodeGroup"));
        
        // Verify packaging types exist
        boolean hasWar = nodes.stream()
            .anyMatch(node -> "war".equals(node.get("packaging")));
        boolean hasJar = nodes.stream()
            .anyMatch(node -> "jar".equals(node.get("packaging")));
            
        assertTrue(hasWar, "Should have at least one WAR project");
        assertTrue(hasJar, "Should have at least one JAR project");
        
        // Print summary
        long warCount = nodes.stream()
            .filter(node -> "war".equals(node.get("packaging")))
            .count();
        long jarCount = nodes.stream()
            .filter(node -> "jar".equals(node.get("packaging")))
            .count();
            
        System.out.println("Total nodes: " + nodes.size());
        System.out.println("WAR projects: " + warCount);
        System.out.println("JAR projects: " + jarCount);
        
        // Verify links structure
        List<Map<String, Object>> links = (List<Map<String, Object>>) data.get("links");
        if (!links.isEmpty()) {
            Map<String, Object> firstLink = links.get(0);
            assertTrue(firstLink.containsKey("source"));
            assertTrue(firstLink.containsKey("target"));
        }
    }
    
    @Test
    void testStaticResources() {
        String url = "http://localhost:" + port + "/";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        String html = response.getBody();
        
        // Verify HTML contains necessary elements
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Dependencies Analyzer"));
        assertTrue(html.contains("d3.v7.min.js"));
        
        // Verify filter functions are present
        assertTrue(html.contains("filterByProject"));
        assertTrue(html.contains("filterByRepository"));
        assertTrue(html.contains("clearFilter"));
        
        // Verify packaging type visualization elements
        assertTrue(html.contains("WAR"));
        assertTrue(html.contains("JAR"));
    }
}