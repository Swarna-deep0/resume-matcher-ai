package com.resumematcher.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        String database;
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            database = "UP";
        } catch (Exception e) {
            database = "DOWN";
        }
        return Map.of("status", "UP", "database", database);
    }
}