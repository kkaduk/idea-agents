package io.a2a.poc.agents.idea.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.a2a.poc.agents.idea.service.IdeaProductWorkflowOrchestrator;
import reactor.core.publisher.Mono;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/banking/products")
@Slf4j
public class BankingProductController {

    @Autowired
    private IdeaProductWorkflowOrchestrator orchestrator;


    @PostMapping("/develop")
    public Mono<ResponseEntity<String>> developNewProduct(@RequestBody String newLegislation) {
        log.info("Received product development request");
        return Mono.fromFuture(orchestrator.orchestrateProductDevelopment(newLegislation))
                  .map(result -> ResponseEntity.ok(result))
                  .onErrorReturn(ResponseEntity.internalServerError().body("Workflow execution failed"));
    }

    @GetMapping("/status")
    public ResponseEntity<String> getSystemStatus() {
        return ResponseEntity.ok("Banking Product Development Orchestration Service - Active");
    }

    @GetMapping("/agents/status")
    public ResponseEntity<String> getAgentsStatus() {
        return ResponseEntity.ok("""
            Agent Status:
            - IdeaCreatorAgent: http://localhost:8081
            - RiskEstimatorAgent: http://localhost:8082
            - IdeaCriticAgent: http://localhost:8083
            - IdeaFinalizerAgent: http://localhost:8084
            - HumanAgent: http://localhost:8085
            - OrchestrationService: http://localhost:8080
            """);
    }
}