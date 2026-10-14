package com.sgu.audioguide.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> checkHealth() {
        return Map.of(
            "status", "OK",
            "message", "SGU Audio Guide Backend is running"
        );
    }
}