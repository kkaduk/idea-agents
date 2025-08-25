package io.a2a.poc.agents.idea.service;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.a2a.poc.agents.idea.service.ProductIdeaWorkflowOrchestrator.A2AReceptionistSkill;
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

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductIdeaWorkflowOrchestrator {

        private final WebClient webClient;

        @Autowired
        private PlannerService plannerService;

        @Autowired
        Receptionist receptionist;

        @Autowired
        DispacherAndExecutionService execution;

        public Mono<List<AgentSkillDocument>> discoverAgents(SkillsSearch criterias) {
                log.info("Discovering agents for product development workflow");

                A2ASkillQuery queryAll = A2ASkillQuery.builder()
                                .keywords(criterias.getKeywords())
                                .requiredTags(criterias.getRequiredTags())
                                .maxResults(5)
                                .build();
                Mono<List<AgentSkillDocument>> allSkills = receptionist.findAgentsBySkills(queryAll);

                log.info("Agents discovered successfully: {}", allSkills);
                return allSkills;
        }

        public Mono<List<A2AReceptionistSkill>> getA2AAgentSkills(SkillsSearch criterias) {
                return discoverAgents(criterias)
                                .map(agentDocs -> agentDocs.stream()
                                                .flatMap(doc -> doc.getSkills().stream()
                                                                .map(skill -> new A2AReceptionistSkill(
                                                                                skill.getId(),
                                                                                doc.getAgentName(),
                                                                                doc.getUrl(),
                                                                                skill.getDescription(),
                                                                                doc.getConfidence(),
                                                                                skill.getId())))
                                                .toList())
                                .doOnNext(skills -> log.info("A2A Agent Skills discovered: {}", skills));
        }

        public Mono<String> skillToExecute(String newIdea) {
        //   public Mono<String> orchestrateProductDevelopment(String newIdea) {
                // String sessionId = UUID.randomUUID().toString();
                double minConfidence = 0.1;

                Mono<SkillsSearch> skillsSearchMono = plannerService.prepareAgentSkills(newIdea)
                        .defaultIfEmpty(Map.of())
                        .map(map -> new SkillsSearch(
                                (List<String>) SkillsSearchMapper.toStringList(map.get("keywords")),
                                (List<String>) SkillsSearchMapper.toStringList(map.get("requiredTags"))
                ));

                return skillsSearchMono
                        .flatMap(this::getA2AAgentSkills)
                        .map(skills -> skills.stream()
                                .filter(skill -> skill.confidence != null && skill.confidence >= minConfidence)
                                .toList())
                        .flatMap(filteredSkills -> {
                                UserTask task = new UserTask(
                                        "Draft a new banking savings product from fresh legislation",
                                        "Analyze the new consumer credit amendment 2025 and propose a product idea suitable for millennials.",
                                        Map.of("country", "PL", "segment", "millennials"));
                                return plannerService.plan(task, filteredSkills, minConfidence)
                                        .map(planMap -> {
                                                try {
                                                        return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(planMap);
                                                } catch (Exception e) {
                                                        return "{\"serialization_error\": \"" + e.getMessage() + "\"}";
                                                }
                                        });
                        });
                        
        }

        public Mono<String> orchestrateProductDevelopment(String idea){
                return skillToExecute(idea)
                        .flatMap(planJson -> {
                                try {
                                        TaskOrchestrationResponse response =
                                                new com.fasterxml.jackson.databind.ObjectMapper()
                                                        .readValue(planJson, TaskOrchestrationResponse.class);
                                        String result = execution.dispatchAndExecuteTask(response);
                                        return Mono.just(result);
                                } catch (Exception e) {
                                        return Mono.error(e);
                                }
                        });
        }

     
        public record A2AReceptionistSkill(
                        String id,
                        String agentName,
                        String agentUrl,
                        String skillDescription,
                        Double confidence,
                        String skilId) {
        }
}
