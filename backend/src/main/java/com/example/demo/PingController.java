package com.example.demo;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class PingController {

    @Value("${spring.application.name}")
    private String appName;

    @Value("${KAFKA_BOOTSTRAP_SERVERS:kafka:9092}")
    private String kafkaBootstrapServers;

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("app", appName);
        response.put("time", Instant.now().toString());
        response.put("kafkaBootstrapServers", kafkaBootstrapServers);
        return response;
    }
}
