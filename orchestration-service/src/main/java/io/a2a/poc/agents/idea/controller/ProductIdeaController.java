package io.a2a.poc.agents.idea.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.a2a.poc.agents.idea.service.ProductIdeaWorkflowOrchestrator;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/products/idea")
@Slf4j
public class ProductIdeaController {

    @Autowired
    private ProductIdeaWorkflowOrchestrator orchestrator;


    @PostMapping("/develop")
    public Mono<ResponseEntity<String>> developNewProduct(@RequestBody String oportunity) {
        log.info("Received product idea development request");
        return orchestrator.orchestrateProductDevelopment(oportunity)
                  .map(result -> ResponseEntity.ok(result))
                  .onErrorReturn(ResponseEntity.internalServerError().body("Workflow execution failed"));
    }
}
