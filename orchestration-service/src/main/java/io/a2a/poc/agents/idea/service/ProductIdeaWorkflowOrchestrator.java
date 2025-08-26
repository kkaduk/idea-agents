package io.a2a.poc.agents.idea.service;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import io.a2a.poc.agents.idea.service.mapper.SkillsSearchMapper;
import io.a2a.poc.agents.idea.service.model.SkillsSearch;
import io.a2a.poc.agents.idea.service.model.TaskOrchestrationResponse;
import io.a2a.poc.agents.idea.util.PlannerPromptBuilder.UserTask;
import io.a2a.poc.agents.idea.util.PlannerService;
import io.a2a.receptionist.Receptionist;
import io.a2a.receptionist.model.A2ASkillQuery;
import io.a2a.receptionist.model.AgentSkillDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIdeaWorkflowOrchestrator {
            
    private static final double MIN_CONFIDENCE_THRESHOLD = 0.1;
    private static final int MAX_AGENT_RESULTS = 5;
    private static final String CORRELATION_ID_KEY = "correlationId";
    
    private final PlannerService plannerService;
    private final Receptionist receptionist;
    private final DispatcherAndExecutionService execution;

    /**
     * Discovers agents based on skill criteria with enhanced logging and error handling
     */
    public Mono<List<AgentSkillDocument>> discoverAgents(SkillsSearch criteria, String correlationId) {
        log.info("[{}] Starting agent discovery for keywords: {}, tags: {}", 
                correlationId, criteria.getKeywords(), criteria.getRequiredTags());

        A2ASkillQuery queryAll = A2ASkillQuery.builder()
                .keywords(criteria.getKeywords())
                .requiredTags(criteria.getRequiredTags())
                .maxResults(MAX_AGENT_RESULTS)
                .build();

        return receptionist.findAgentsBySkills(queryAll)
                .doOnSubscribe(subscription -> 
                    log.debug("[{}] Executing receptionist query: {}", correlationId, queryAll))
                .doOnNext(agents -> 
                    log.info("[{}] Discovered {} agents", correlationId, agents.size()))
                .doOnError(error -> 
                    log.error("[{}] Failed to discover agents", correlationId, error))
                .retryWhen(Retry.backoff(3, Duration.ofMillis(500))
                    .doBeforeRetry(retrySignal -> 
                        log.warn("[{}] Retrying agent discovery, attempt: {}", 
                                correlationId, retrySignal.totalRetries() + 1)))
                .onErrorMap(throwable -> 
                    new RuntimeException("Agent discovery failed after retries", throwable));
    }

    /**
     * Maps agent documents to A2A receptionist skills with validation
     */
    public Mono<List<A2AReceptionistSkill>> getA2AAgentSkills(SkillsSearch criteria, String correlationId) {
        log.debug("[{}] Converting agent documents to A2A skills", correlationId);
        
        return discoverAgents(criteria, correlationId)
                .map(agentDocs -> agentDocs.stream()
                        .flatMap(doc -> {
                            if (doc.getSkills() == null || doc.getSkills().isEmpty()) {
                                log.warn("[{}] Agent {} has no skills", correlationId, doc.getAgentName());
                                return java.util.stream.Stream.empty();
                            }
                            return doc.getSkills().stream()
                                    .map(skill -> new A2AReceptionistSkill(
                                            skill.getId(),
                                            doc.getAgentName(),
                                            doc.getUrl(),
                                            skill.getDescription(),
                                            doc.getConfidence(),
                                            skill.getId()));
                        })
                        .collect(Collectors.toList()))
                .doOnNext(skills -> 
                    log.info("[{}] Converted {} agent documents to {} A2A skills", 
                            correlationId, skills.size(), skills.size()))
                .doOnError(error -> 
                    log.error("[{}] Failed to convert agent documents to A2A skills", correlationId, error));
    }

    /**
     * Determines which skills to execute based on the idea with enhanced flow control
     */
    public Mono<TaskOrchestrationResponse> determineSkillsToExecute(String newIdea, String correlationId) {
        log.info("[{}] Determining skills to execute for idea: {}", correlationId, 
                newIdea.substring(0, Math.min(newIdea.length(), 100)) + "...");

        return prepareSkillsSearch(newIdea, correlationId)
                .flatMap(skillsSearch -> getFilteredSkills(skillsSearch, correlationId))
                .flatMap(filteredSkills -> createExecutionPlan(newIdea, filteredSkills, correlationId))
                .doOnError(error -> 
                    log.error("[{}] Failed to determine skills to execute", correlationId, error));
    }

    /**
     * Prepares skills search criteria from the idea
     */
    private Mono<SkillsSearch> prepareSkillsSearch(String newIdea, String correlationId) {
        log.debug("[{}] Preparing skills search from idea", correlationId);
        
        return plannerService.prepareAgentSkills(newIdea)
                .defaultIfEmpty(Map.of())
                .map(map -> {
                    List<String> keywords = SkillsSearchMapper.toStringList(map.get("keywords"));
                    List<String> requiredTags = SkillsSearchMapper.toStringList(map.get("requiredTags"));
                    
                    log.debug("[{}] Extracted keywords: {}, tags: {}", correlationId, keywords, requiredTags);
                    return new SkillsSearch(keywords, requiredTags);
                })
                .doOnError(error -> 
                    log.error("[{}] Failed to prepare skills search", correlationId, error));
    }

    /**
     * Gets and filters skills based on confidence threshold
     */
    private Mono<List<A2AReceptionistSkill>> getFilteredSkills(SkillsSearch skillsSearch, String correlationId) {
        return getA2AAgentSkills(skillsSearch, correlationId)
                .map(skills -> {
                    List<A2AReceptionistSkill> filteredSkills = skills.stream()
                            .filter(skill -> skill.confidence != null && skill.confidence >= MIN_CONFIDENCE_THRESHOLD)
                            .collect(Collectors.toList());
                    
                    log.info("[{}] Filtered {} skills to {} based on confidence threshold {}", 
                            correlationId, skills.size(), filteredSkills.size(), MIN_CONFIDENCE_THRESHOLD);
                    
                    if (filteredSkills.isEmpty()) {
                        log.warn("[{}] No skills meet the confidence threshold", correlationId);
                    }
                    
                    return filteredSkills;
                });
    }

    /**
     * Creates execution plan from idea and filtered skills
     */
    private Mono<TaskOrchestrationResponse> createExecutionPlan(String newIdea, 
                                                               List<A2AReceptionistSkill> filteredSkills, 
                                                               String correlationId) {
        log.debug("[{}] Creating execution plan", correlationId);
        
        return plannerService.createIdea(newIdea)
                .flatMap(ideaMap -> {
                    String idea = extractStringValue(ideaMap, "description", correlationId);
                    String skills = extractStringValue(ideaMap, "skills", correlationId);
                    
                    Map<String, Object> skillsMap = parseSkillsToMap(skills, correlationId);
                    
                    UserTask task = new UserTask(
                            "Draft a new banking product from fresh legislation",
                            idea,
                            skillsMap);
                    
                    log.debug("[{}] Created user task with {} skills", correlationId, skillsMap.size());
                    
                    return plannerService.plan(task, filteredSkills, MIN_CONFIDENCE_THRESHOLD)
                            .map(planMap -> convertToTaskOrchestrationResponse(planMap, correlationId));
                })
                .doOnError(error -> 
                    log.error("[{}] Failed to create execution plan", correlationId, error));
    }

    /**
     * Orchestrates the complete product development workflow
     */
    public Mono<String> orchestrateProductDevelopment(String idea) {
        String correlationId = generateCorrelationId();
        log.info("[{}] Starting product development orchestration for idea", correlationId);
        
        return determineSkillsToExecute(idea, correlationId)
                .flatMap(response -> {
                    log.info("[{}] Dispatching execution for orchestrated plan", correlationId);
                    return execution.dispatchAndExecuteTask(response)
                            .doOnNext(result -> 
                                log.info("[{}] Product development orchestration completed successfully", correlationId))
                            .doOnError(error -> 
                                log.error("[{}] Product development orchestration failed", correlationId, error));
                })
                .onErrorMap(throwable -> 
                    new RuntimeException("Product development orchestration failed", throwable));
    }

    // Helper methods
    
    private String extractStringValue(Map<String, Object> map, String key, String correlationId) {
        Object value = map.get(key);
        if (value == null) {
            log.warn("[{}] Missing required key '{}' in response map", correlationId, key);
            return "";
        }
        return value.toString();
    }

    private Map<String, Object> parseSkillsToMap(String skills, String correlationId) {
        try {
            return Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toMap(s -> s, s -> s));
        } catch (Exception e) {
            log.warn("[{}] Failed to parse skills string: {}", correlationId, skills, e);
            return Map.of();
        }
    }

    private TaskOrchestrationResponse convertToTaskOrchestrationResponse(Map<String, Object> planMap, String correlationId) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper()
                    .convertValue(planMap, TaskOrchestrationResponse.class);
        } catch (Exception e) {
            log.error("[{}] Failed to convert planMap to TaskOrchestrationResponse", correlationId, e);
            throw new RuntimeException("Failed to convert planMap to TaskOrchestrationResponse", e);
        }
    }

    private String generateCorrelationId() {
        return java.util.UUID.randomUUID().toString().substring(0, 8);
    }

    // Fixed record name (was "skilId" -> "skillId")
    public record A2AReceptionistSkill(
            String id,
            String agentName,
            String agentUrl,
            String skillDescription,
            Double confidence,
            String skillId) {
    }
}