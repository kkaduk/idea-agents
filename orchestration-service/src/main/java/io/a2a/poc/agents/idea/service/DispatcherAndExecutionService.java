package io.a2a.poc.agents.idea.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import io.a2a.poc.agents.idea.service.model.TaskOrchestrationResponse;
import io.a2a.receptionist.Receptionist;
import io.a2a.receptionist.model.SkillInvocationRequest;
import io.a2a.receptionist.model.SkillInvocationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatcherAndExecutionService {

    @Autowired
    Receptionist receptionist;

    public reactor.core.publisher.Mono<String> dispatchAndExecuteTask(TaskOrchestrationResponse orchestrationResponse) {
        return reactor.core.publisher.Mono.fromCallable(() -> {
            try {
                Map<String, Object> results = new HashMap<>();
                List<TaskOrchestrationResponse.SelectedSkill> skills = orchestrationResponse.selectedSkills();
                List<TaskOrchestrationResponse.SelectedSkill> executionOrder = topologicalSort(skills);
                results = executeInDependencyOrder(executionOrder);
                return consolidateResults(results, orchestrationResponse.taskId());
            } catch (Exception e) {
                log.error("Error executing task orchestration: {}", e.getMessage(), e);
                return String.format("Task execution failed: %s", e.getMessage());
            }
        });
    }

    private List<TaskOrchestrationResponse.SelectedSkill> topologicalSort(
            List<TaskOrchestrationResponse.SelectedSkill> skills) {
        
        Map<String, TaskOrchestrationResponse.SelectedSkill> skillMap = skills.stream()
                .collect(Collectors.toMap(TaskOrchestrationResponse.SelectedSkill::stepId, skill -> skill));

        Map<String, Integer> inDegree = new HashMap<>();
        Map<String, List<String>> adjacencyList = new HashMap<>();

        for (TaskOrchestrationResponse.SelectedSkill skill : skills) {
            inDegree.put(skill.stepId(), 0);
            adjacencyList.put(skill.stepId(), new ArrayList<>());
        }

        // Build the graph
        for (TaskOrchestrationResponse.SelectedSkill skill : skills) {
            if (skill.dependsOn() != null) {
                for (String dependency : skill.dependsOn()) {
                    if (!skillMap.containsKey(dependency)) {
                        throw new IllegalArgumentException(
                                String.format("Dependency '%s' for step '%s' does not exist", dependency,
                                        skill.stepId()));
                    }
                    adjacencyList.get(dependency).add(skill.stepId());
                    inDegree.put(skill.stepId(), inDegree.get(skill.stepId()) + 1);
                }
            }
        }

        // Kahn's algorithm for topological sorting
        Queue<String> queue = new LinkedList<>();
        List<TaskOrchestrationResponse.SelectedSkill> result = new ArrayList<>();

        // Add all nodes with in-degree 0 to queue
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        while (!queue.isEmpty()) {
            String currentStepId = queue.poll();
            TaskOrchestrationResponse.SelectedSkill currentSkill = skillMap.get(currentStepId);
            result.add(currentSkill);

            // Process all adjacent nodes
            for (String adjacentStepId : adjacencyList.get(currentStepId)) {
                inDegree.put(adjacentStepId, inDegree.get(adjacentStepId) - 1);
                if (inDegree.get(adjacentStepId) == 0) {
                    queue.offer(adjacentStepId);
                }
            }
        }

        // Check for cycles
        if (result.size() != skills.size()) {
            throw new IllegalArgumentException("Circular dependency detected in task graph");
        }

        return result;
    }

    private Map<String, Object> executeInDependencyOrder(List<TaskOrchestrationResponse.SelectedSkill> orderedSkills) {
        Map<String, Object> results = new LinkedHashMap<>();

        for (TaskOrchestrationResponse.SelectedSkill skill : orderedSkills) {
            try {
                log.info("Executing step: {} with agent: {} and skill: {}",
                        skill.stepId(), skill.agentName(), skill.skillId());

                Object result = executeSkillWithRetry(skill, results);
                results.put(skill.stepId(), result);

                log.info("Successfully completed step: {}", skill.stepId());

            } catch (Exception e) {
                log.error("Failed to execute step: {} - {}", skill.stepId(), e.getMessage());
                results.put(skill.stepId(), "Error: " + e.getMessage());

                // FIXME: (KK) For now, we continue with remaining tasks
            }
        }

        return results;
    }

    private Object executeSkillWithRetry(TaskOrchestrationResponse.SelectedSkill skill,
            Map<String, Object> previousResults) {
        int maxAttempts = skill.retries() != null && skill.retries().maxAttempts() != null
                ? skill.retries().maxAttempts()
                : 1;
        int backoffSec = skill.retries() != null && skill.retries().backoffSec() != null
                ? skill.retries().backoffSec()
                : 1;

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeSkill(skill, previousResults);
            } catch (Exception e) {
                lastException = e;
                log.warn("Attempt {}/{} failed for step {}: {}",
                        attempt, maxAttempts, skill.stepId(), e.getMessage());

                if (attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffSec * 1000L);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during retry backoff", ie);
                    }
                }
            }
        }

        throw new RuntimeException(
                String.format("Failed after %d attempts for step %s", maxAttempts, skill.stepId()),
                lastException);
    }

    private Object executeSkill(TaskOrchestrationResponse.SelectedSkill skill, Map<String, Object> previousResults) {
    
        List<String> consolidatedInput = new ArrayList<>();

        if (skill.input() != null && !skill.input().isEmpty()) {
            skill.input().forEach((key, value) -> {
                consolidatedInput.add(String.format("%s: %s", key, value != null ? value.toString() : "null"));
            });
        }

        if (skill.dependsOn() != null) {
            for (String dependency : skill.dependsOn()) {
                Object dependencyResult = previousResults.get(dependency);
                if (dependencyResult != null) {
                    consolidatedInput.add(String.format("Output from step '%s': %s", dependency, dependencyResult));
                } else {
                    log.warn("No result found for dependency: {} in step: {}", dependency, skill.stepId());
                }
            }
        }

        if (consolidatedInput.isEmpty()) {
            consolidatedInput.add("No specific input provided for this skill execution");
        }

        SkillInvocationRequest skillRequest = SkillInvocationRequest.builder()
                .agentName(skill.agentName())
                .skillId(skill.skillId())
                .input(consolidatedInput)
                .contextId(generateContextId(skill))
                .metadata(createMetadata(skill))
                .build();

        SkillInvocationResponse response;
        int timeoutSec = skill.timeoutSec() != null ? skill.timeoutSec() : 120; // default 2 minutes

        try {
            response = receptionist.invokeAgentSkill(skillRequest)
                    .timeout(Duration.ofSeconds(timeoutSec))
                    .block();
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format("Skill invocation failed for %s:%s - %s",
                            skill.agentName(), skill.skillId(), e.getMessage()),
                    e);
        }

        if (response != null && response.getResult() != null) {
            return response.getResult();
        } else {
            throw new RuntimeException(
                    String.format("No result received from skill %s:%s",
                            skill.agentName(), skill.skillId()));
        }
    }

    private String generateContextId(TaskOrchestrationResponse.SelectedSkill skill) {
        return String.format("ctx_%s_%s_%d", skill.agentName(), skill.stepId(), System.currentTimeMillis());
    }

    private Map<String, Object> createMetadata(TaskOrchestrationResponse.SelectedSkill skill) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("stepId", skill.stepId());
        metadata.put("agentName", skill.agentName());
        metadata.put("skillId", skill.skillId());
        metadata.put("confidence", skill.confidence());
        metadata.put("priority", skill.priority());
        metadata.put("executionTimestamp", System.currentTimeMillis());

        if (skill.agentUrl() != null) {
            metadata.put("agentUrl", skill.agentUrl().toString());
        }

        return metadata;
    }

    private String consolidateResults(Map<String, Object> results, String taskId) {
        StringBuilder consolidatedResult = new StringBuilder();
        consolidatedResult.append(String
                .format("Task '%s' execution completed using dependency-based acyclic graph execution.\n\n", taskId));

        long successfulSteps = results.values().stream()
                .filter(result -> !result.toString().startsWith("Error:"))
                .count();

        consolidatedResult.append(String.format("Execution Summary: %d/%d steps completed successfully\n\n",
                successfulSteps, results.size()));

        // Add individual step results in execution order
        consolidatedResult.append("Step-by-Step Results:\n");
        results.forEach((stepId, result) -> {
            String status = result.toString().startsWith("Error:") ? "[FAILED]" : "[SUCCESS]";
            consolidatedResult.append(String.format("%s Step %s: %s\n", status, stepId, result));
        });

        consolidatedResult.append("\n" + "=".repeat(50) + "\n");
        consolidatedResult.append("Final Consolidated Output:\n");

        // Create a comprehensive summary combining all successful results
        String successfulResults = results.entrySet().stream()
                .filter(entry -> !entry.getValue().toString().startsWith("Error:"))
                .map(entry -> String.format("From %s: %s", entry.getKey(), entry.getValue()))
                .collect(Collectors.joining("\n\n"));

        if (!successfulResults.isEmpty()) {
            consolidatedResult.append(successfulResults);
        } else {
            consolidatedResult.append("No successful results to consolidate - all steps failed.");
        }

        return consolidatedResult.toString();
    }
}
