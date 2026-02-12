package com.alang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application entry point for ALang language learning backend.
 *
 * Architecture overview:
 * - Controllers: Thin HTTP layer, no business logic
 * - Services: Business logic, orchestration, LLM interaction
 * - LLMService: CENTRALIZED LLM communication (only place that calls external LLM API)
 * - Repositories: Data access layer
 *
 * Key design principles:
 * 1. Frontend NEVER talks directly to LLM
 * 2. ALL LLM logic is centralized in LLMService
 * 3. Token usage is controlled and tracked
 * 4. Chat history is SUMMARIZED, not stored raw
 */
@SpringBootApplication
public class AlangApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlangApplication.class, args);
    }
}
