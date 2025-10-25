package com.jigmjugm.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthCheckController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "jigmjugm");
        return ResponseEntity.ok(health);
    }

    @GetMapping("/db")
    public ResponseEntity<Map<String, Object>> databaseHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        try {
            // PostgreSQL 연결 테스트
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            
            // user_account 테이블 카운트
            Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM user_account", Integer.class
            );
            
            health.put("status", "UP");
            health.put("database", "PostgreSQL");
            health.put("connection", "OK");
            health.put("userCount", userCount);
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            health.put("status", "DOWN");
            health.put("database", "PostgreSQL");
            health.put("connection", "FAILED");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(503).body(health);
        }
    }

    @GetMapping("/detailed")
    public ResponseEntity<Map<String, Object>> detailedHealthCheck() {
        Map<String, Object> health = new HashMap<>();
        
        // 서비스 정보
        health.put("service", "jigmjugm");
        health.put("version", "0.0.1-SNAPSHOT");
        health.put("timestamp", LocalDateTime.now());
        
        // JVM 정보
        Map<String, Object> jvm = new HashMap<>();
        Runtime runtime = Runtime.getRuntime();
        jvm.put("totalMemory", runtime.totalMemory() / 1024 / 1024 + " MB");
        jvm.put("freeMemory", runtime.freeMemory() / 1024 / 1024 + " MB");
        jvm.put("maxMemory", runtime.maxMemory() / 1024 / 1024 + " MB");
        jvm.put("processors", runtime.availableProcessors());
        health.put("jvm", jvm);
        
        // 시스템 정보
        Map<String, Object> system = new HashMap<>();
        system.put("javaVersion", System.getProperty("java.version"));
        system.put("javaVendor", System.getProperty("java.vendor"));
        system.put("osName", System.getProperty("os.name"));
        system.put("osVersion", System.getProperty("os.version"));
        health.put("system", system);
        
        // 데이터베이스 상태
        Map<String, Object> database = new HashMap<>();
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            database.put("status", "UP");
            database.put("type", "PostgreSQL");
        } catch (Exception e) {
            database.put("status", "DOWN");
            database.put("error", e.getMessage());
        }
        health.put("database", database);
        
        // 전체 상태
        boolean isHealthy = "UP".equals(database.get("status"));
        health.put("status", isHealthy ? "UP" : "DEGRADED");
        
        return ResponseEntity.ok(health);
    }
}