package io.a2a.poc.agents.idea.service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

        // private final WebClient webClient;

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

        public Mono<TaskOrchestrationResponse> skillToExecute(String newIdea) {
                double minConfidence = 0.1;

                Mono<SkillsSearch> skillsSearchMono = plannerService.prepareAgentSkills(newIdea)
                                .defaultIfEmpty(Map.of())
                                .map(map -> new SkillsSearch(
                                                (List<String>) SkillsSearchMapper.toStringList(map.get("keywords")),
                                                (List<String>) SkillsSearchMapper
                                                                .toStringList(map.get("requiredTags"))));

                return skillsSearchMono
                                .flatMap(this::getA2AAgentSkills)
                                .map(skills -> skills.stream()
                                                .filter(skill -> skill.confidence != null
                                                                && skill.confidence >= minConfidence)
                                                .toList())
                                .flatMap(filteredSkills -> plannerService.createIdea(newIdea).flatMap(ideaMap -> {
                                        String idea = (String) ideaMap.get("description");
                                        String skills = (String) ideaMap.get("skills");
                                        Map<String, Object> skillsMap = Arrays.stream(skills.split(","))
                                                        .map(String::trim)
                                                        .collect(Collectors.toMap(s -> s, s -> s));

                                        UserTask task = new UserTask(
                                                        "Draft a new banking product from fresh legislation",
                                                        idea,
                                                        skillsMap);
                                        return plannerService.plan(task, filteredSkills, minConfidence)
                                                        .map(planMap -> {
                                                                try {
                                                                        return new com.fasterxml.jackson.databind.ObjectMapper()
                                                                                        .convertValue(planMap,
                                                                                                        TaskOrchestrationResponse.class);
                                                                } catch (Exception e) {
                                                                        throw new RuntimeException(
                                                                                        "Failed to convert planMap to TaskOrchestrationResponse",
                                                                                        e);
                                                                }
                                                        });
                                }));

        }

        public Mono<String> orchestrateProductDevelopment(String idea) {
                return skillToExecute(idea)
                                .map(response -> execution.dispatchAndExecuteTask(response));
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
