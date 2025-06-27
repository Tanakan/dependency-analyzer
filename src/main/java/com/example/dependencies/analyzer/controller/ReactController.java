package com.example.dependencies.analyzer.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReactController {
    
    @GetMapping("/react")
    public String react() {
        return "forward:/react/index.html";
    }
    
    @GetMapping("/react/**")
    public String reactRoutes() {
        return "forward:/react/index.html";
    }
}