package com.example.demo;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class DatabasePingController {
    private final DatabasePingService databasePingService;

    public DatabasePingController(DatabasePingService databasePingService) {
        this.databasePingService = databasePingService;
    }

    @GetMapping("/db-ping")
    public Map<String, Object> dbPing() {
        return databasePingService.pingDatabase();
    }
}
