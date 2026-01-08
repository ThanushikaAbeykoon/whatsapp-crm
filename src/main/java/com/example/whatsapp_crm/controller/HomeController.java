package com.example.whatsapp_crm.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HomeController {

    // Root endpoint - API info
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
            "status", "running",
            "message", "WhatsApp CRM API is running",
            "version", "1.0.0",
            "baseUrl", "/api",
            "endpoints", Map.of(
                "messages", "/api/messages",
                "contacts", "/api/contacts",
                "send", "/api/send",
                "webhook", "/api/webhook",
                "health", "/api/health"
            ),
            "documentation", "See API_DOCUMENTATION.md for detailed API documentation"
        ));
    }
}
