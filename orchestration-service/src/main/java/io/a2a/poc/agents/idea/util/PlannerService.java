package io.a2a.poc.agents.idea.util;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.a2a.poc.agents.idea.service.ChatService;
import io.a2a.poc.agents.idea.service.ProductIdeaWorkflowOrchestrator.A2AReceptionistSkill;
import io.a2a.receptionist.Receptionist;
import io.a2a.receptionist.model.SkillInvocationRequest;
import io.a2a.receptionist.model.SkillInvocationResponse;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class PlannerService {

    private final ChatService chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    Receptionist receptionist;

    public PlannerService(ChatService chatClient) {
        this.chatClient = chatClient;
    }

    public Mono<Map<String, Object>> plan(PlannerPromptBuilder.UserTask task,
            List<A2AReceptionistSkill> catalog,
            double minConfidence) {

        String prompt = PlannerPromptBuilder.build(task, catalog, minConfidence);

        return chatClient.ask(prompt)
                .map(response -> {
                    try {
                        return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
                        });
                    } catch (JsonProcessingException e) {
                        return Map.of("raw", response);
                    }
                });
    }

    public Mono<Map<String, Object>> createIdea(String document) {

        String prompt = PlannerPromptBuilder.buildSkillsPrompt(document);

        return chatClient.ask(prompt)
                .map(response -> {
                    try {
                        return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
                        });
                    } catch (JsonProcessingException e) {
                        return Map.of("raw", response);
                    }
                });
    }

    public Mono<Map<String, Object>> prepareAgentSkills(String idea) {

        String prompt = PlannerPromptBuilder.buildSkillsPrompt(idea);

        return chatClient.ask(prompt)
                .map(response -> {
                    try {
                        return objectMapper.readValue(response, new TypeReference<Map<String, Object>>() {
                        });
                    } catch (JsonProcessingException e) {
                        return Map.of("raw", response);
                    }
                });
    }

    // ****************For future use ********** */

    private CompletableFuture<String> invokeSkillByReceptionist(String agentName, String skillId,
            List<String> input) {
        log.debug("Invoking skill {} with agent {}", skillId, agentName);

        // Create request payload for A2A agent
        SkillInvocationRequest skillRequest = SkillInvocationRequest.builder()
                .agentName(agentName)
                .skillId(skillId)
                .input(input)
                .build();

        // Send request to receptionist
        Mono<SkillInvocationResponse> responseMono = receptionist.invokeAgentSkill(skillRequest);
        SkillInvocationResponse response = responseMono.block();

        if (response != null && response.getSuccess()) {
            return CompletableFuture.completedFuture(response.getResult().getParts().stream()
                    .map(part -> part instanceof TextPart ? ((TextPart) part).getText() : "")
                    .collect(Collectors.joining(", ")));
        } else {
            log.error("Skill invocation failed: "
                    + (response != null ? response.getErrorMessage() : "No response"));
            return CompletableFuture.completedFuture(
                    "SKILL_INVOCATION_ERROR: " + (response != null ? response.getErrorMessage()
                            : "No response"));
        }
    }

}
