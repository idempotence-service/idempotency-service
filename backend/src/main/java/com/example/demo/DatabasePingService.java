package com.example.demo;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class DatabasePingService {
    private final JdbcTemplate jdbcTemplate;

    public DatabasePingService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> pingDatabase() {
        Map<String, Object> response = new LinkedHashMap<>();
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            String database = jdbcTemplate.queryForObject("select current_database()", String.class);
            String user = jdbcTemplate.queryForObject("select current_user", String.class);

            response.put("status", "ok");
            response.put("select1", one);
            response.put("database", database);
            response.put("user", user);
        } catch (DataAccessException ex) {
            response.put("status", "error");
            response.put("message", ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
        }
        return response;
    }
}
