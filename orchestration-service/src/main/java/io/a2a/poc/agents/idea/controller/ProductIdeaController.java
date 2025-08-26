package io.a2a.poc.agents.idea.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.a2a.poc.agents.idea.service.ProductIdeaWorkflowOrchestrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ProductIdeaController {
    
    private final ProductIdeaWorkflowOrchestrator orchestrator;
    
    @PostMapping(value = "/api/product-ideas/orchestrate", 
                 produces = MediaType.TEXT_PLAIN_VALUE)
    public Mono<ResponseEntity<String>> orchestrateProductDevelopment(@RequestBody String idea) {
        String correlationId = generateCorrelationId();
        log.info("[{}] Received product development request", correlationId);
        
        return orchestrator.orchestrateProductDevelopment(idea)
                .map(result -> {
                    log.info("[{}] Successfully completed product development orchestration", correlationId);
                    return ResponseEntity.ok()
                            .contentType(MediaType.TEXT_PLAIN)
                            .body(result);
                })
                .onErrorResume(error -> {
                    log.error("[{}] Product development orchestration failed", correlationId, error);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.TEXT_PLAIN)
                            .body("Orchestration failed: " + error.getMessage()));
                });
    }
    
    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }
}