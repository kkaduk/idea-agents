package io.a2a.poc.agents.idea.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import io.a2a.poc.agents.idea.util.PlannerService;
import io.a2a.poc.agents.idea.util.PlannerPromptBuilder.UserTask;
import io.a2a.receptionist.Receptionist;
import io.a2a.receptionist.model.A2ASkillQuery;
import io.a2a.receptionist.model.AgentSkillDocument;
import io.a2a.receptionist.model.SkillInvocationRequest;
import io.a2a.receptionist.model.SkillInvocationResponse;
import io.a2a.spec.TextPart;
// import net.kaduk.a2a.AgentCapabilityInfo;
// import net.kaduk.a2a.receptionist.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class IdeaProductWorkflowOrchestrator {

        private final WebClient webClient;


        @Autowired
        private PlannerService plannerService;

        @Value("${agents.idea-creator.url:http://localhost:8081}")
        private String ideaCreatorUrl;

        private String ideaCreatorAgentName = "IdeaCreatorAgent";

        @Value("${agents.risk-estimator.url:http://localhost:8082}")
        private String riskEstimatorUrl;

        private String riskEstimatorAgentName = "RiskEstimatorAgent";

        @Value("${agents.idea-critic.url:http://localhost:8083}")
        private String ideaCriticUrl;

        private String ideaCriticAgentName = "IdeaCriticAgent";

        @Value("${agents.idea-finalizer.url:http://localhost:8084}")
        private String ideaFinalizerUrl;

        private String ideaFinalizerAgentName = "IdeaFinalizerAgent";

        @Value("${agents.human-agent.url:http://localhost:8085}")
        private String humanAgentUrl;

        private String humanAgentAgentName = "HumanAgent";

        @Autowired
        Receptionist receptionist;

        public Mono<List<AgentSkillDocument>> discoverAgents(List<String> keywords, List<String> requiredTags) {
                log.info("Discovering agents for product development workflow");

                A2ASkillQuery queryAll = A2ASkillQuery.builder()
                                .keywords(keywords)
                                .requiredTags(requiredTags)
                                .maxResults(5)
                                .build();
                Mono<List<AgentSkillDocument>> allSkills = receptionist.findAgentsBySkills(queryAll);

                log.info("Agents discovered successfully: {}", allSkills);
                return allSkills;
        }

        public Mono<List<A2AReceptionistSkill>> getA2AAgentSkills(List<String> keywords, List<String> requiredTags) {
                return discoverAgents(keywords, requiredTags)
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

        public CompletableFuture<String> orchestrateProductDevelopment(String newLegislation) {
                String sessionId = UUID.randomUUID().toString();

                // Mono<List<A2AReceptionistSkill>> agentSkills = getA2AAgentSkills(
                // Arrays.asList("product", "development", "banking", "legislation", "AI"),
                // Arrays.asList("product-development", "AI-driven", "banking"));

                // CompletableFuture<String> agentSkillsFuture = getA2AAgentSkills(
                // Arrays.asList("product", "development", "banking", "legislation", "AI"),
                // Arrays.asList("product-development", "AI-driven", "banking"))
                // .map(skills -> {
                // // Convert List<A2AReceptionistSkill> to JSON, CSV, or a String
                // representation
                // return skills.stream()
                // .map(A2AReceptionistSkill::toString) // or any custom formatting
                // .collect(Collectors.joining(", "));
                // })
                // .toFuture();
                double minConfidence = 0.8; // example threshold
                CompletableFuture<List<A2AReceptionistSkill>> agentSkillsFuture = getA2AAgentSkills(
                                Arrays.asList("product", "development", "banking", "legislation", "AI"),
                                Arrays.asList("product-development", "AI-driven", "banking"))
                                .map(skills -> skills.stream()
                                                .filter(skill -> skill.confidence != null
                                                                && skill.confidence >= minConfidence)
                                                .toList())
                                .toFuture();

                UserTask task = new UserTask(
                                "Draft a new retail savings product from fresh legislation",
                                "Analyze the new consumer credit amendment 2025 and propose a product idea suitable for millennials.",
                                Map.of("country", "PL", "segment", "millennials"));

                var plan = plannerService.plan(task, agentSkillsFuture.join(), minConfidence);

                return CompletableFuture.supplyAsync(() -> {
                        try {
                                log.info("Starting AI-driven product development workflow, session: {}", sessionId);

                                // Step 1: Create initial product idea
                                String initialIdea = createInitialProductIdea(newLegislation, sessionId).get();
                                log.info("Initial product idea created for session: {}", sessionId);

                                // Step 2: Multi-agent feedback and iterative refinement loop
                                String finalResult = conductAIFeedbackLoop(initialIdea, sessionId).get();
                                log.info("AI feedback loop completed for session: {}", sessionId);

                                return finalResult;

                        } catch (Exception e) {
                                log.error("Workflow execution failed for session: " + sessionId, e);
                                return "WORKFLOW_ERROR: " + e.getMessage();
                        }
                });
        }

        private CompletableFuture<String> createInitialProductIdea(String legislation, String sessionId) {
                return CompletableFuture.supplyAsync(() -> {
                        try {
                                // First analyze the legislation
                                String analysisResult = invokeSkillByReceptionist(ideaCreatorAgentName,
                                                "analyze-legislation",
                                                List.of(legislation)).get();
                                log.debug("Legislation analysis completed: {}",
                                                analysisResult.substring(0, Math.min(200, analysisResult.length())));

                                // Then create product idea based on analysis
                                String productIdea = invokeSkillByReceptionist(ideaCreatorAgentName,
                                                "create-product-idea",
                                                List.of(analysisResult)).get();
                                log.debug("Product idea created: {}",
                                                productIdea.substring(0, Math.min(200, productIdea.length())));

                                return productIdea;

                        } catch (Exception e) {
                                log.error("Error creating initial product idea for session: " + sessionId, e);
                                throw new RuntimeException("Failed to create initial idea", e);
                        }
                });
        }

        private CompletableFuture<String> conductAIFeedbackLoop(String initialIdea, String sessionId) {
                return CompletableFuture.supplyAsync(() -> {
                        String currentIdea = initialIdea;
                        List<String> developmentHistory = new ArrayList<>();
                        int maxIterations = 2;

                        for (int iteration = 1; iteration <= maxIterations; iteration++) {
                                try {
                                        log.info("Starting iteration {} of {} for session: {}", iteration,
                                                        maxIterations, sessionId);

                                        // Collect comprehensive feedback
                                        List<String> allFeedback = collectComprehensiveFeedback(currentIdea, sessionId)
                                                        .get();
                                        String combinedFeedback = String.join("\n\n", allFeedback);

                                        // Synthesize feedback
                                        String synthesis = invokeSkillByReceptionist(ideaFinalizerAgentName,
                                                        "synthesize-all-feedback",
                                                        List.of(combinedFeedback))
                                                        .get();
                                        developmentHistory.add("Iteration " + iteration + " Synthesis: " + synthesis);

                                        // Make strategic decision
                                        String decision = invokeSkillByReceptionist(ideaFinalizerAgentName,
                                                        "make-strategic-decision",
                                                        List.of(synthesis)).get();
                                        developmentHistory.add("Iteration " + iteration + " Decision: " + decision);

                                        log.info("Iteration {} decision: {}", iteration,
                                                        decision.substring(0, Math.min(100, decision.length())));

                                        if (decision.contains("FINALIZE")) {
                                                // Create final presentation
                                                String developmentJourney = String.join("\n\n", developmentHistory);
                                                String finalPresentation = invokeSkillByReceptionist(
                                                                ideaFinalizerAgentName,
                                                                "create-final-presentation",
                                                                List.of(currentIdea, developmentJourney)).get();

                                                // Process human approval
                                                return processAIHumanApproval(finalPresentation, developmentJourney,
                                                                sessionId).get();

                                        } else if (decision.contains("TERMINATE")) {
                                                log.info("Product development terminated at iteration {} for session: {}",
                                                                iteration,
                                                                sessionId);
                                                return "PROCESS_TERMINATED: " + decision + "\n\nDEVELOPMENT_HISTORY:\n"
                                                                + String.join("\n", developmentHistory);

                                        } else { // ITERATE
                                                 // Refine the product idea
                                                currentIdea = invokeSkillByReceptionist(ideaFinalizerAgentName,
                                                                "refine-product-with-ai",
                                                                List.of(currentIdea, synthesis)).get();
                                                developmentHistory
                                                                .add("Iteration " + iteration
                                                                                + " Refinement: Product idea enhanced based on feedback");

                                                log.info("Product idea refined for iteration {} for session: {}",
                                                                iteration + 1, sessionId);
                                        }

                                } catch (Exception e) {
                                        log.error("Error in iteration {} for session: " + sessionId, iteration, e);
                                        return "ITERATION_ERROR: Failed at iteration " + iteration + " - "
                                                        + e.getMessage();
                                }
                        }

                        log.warn("Maximum iterations reached without finalization for session: {}", sessionId);
                        return "MAX_ITERATIONS_REACHED: Process completed without final approval\n\nDEVELOPMENT_HISTORY:\n"
                                        +
                                        String.join("\n", developmentHistory);
                });
        }

        private CompletableFuture<List<String>> collectComprehensiveFeedback(String productIdea, String sessionId) {
                return CompletableFuture.supplyAsync(() -> {
                        try {
                                List<CompletableFuture<String>> feedbackTasks = new ArrayList<>();

                                // Risk assessment
                                feedbackTasks.add(invokeSkillByReceptionist(riskEstimatorAgentName,
                                                "assess-comprehensive-risk",
                                                List.of(productIdea)));
                                feedbackTasks.add(invokeSkillByReceptionist(riskEstimatorAgentName,
                                                "assess-regulatory-compliance",
                                                List.of(productIdea)));

                                // Critical analysis
                                feedbackTasks.add(invokeSkillByReceptionist(ideaCriticAgentName,
                                                "comprehensive-product-critique",
                                                List.of(productIdea)));
                                feedbackTasks.add(
                                                invokeSkillByReceptionist(ideaCriticAgentName, "competitive-analysis",
                                                                List.of(productIdea)));

                                // Wait for all feedback to complete
                                CompletableFuture<Void> allTasks = CompletableFuture
                                                .allOf(feedbackTasks.toArray(new CompletableFuture[0]));
                                allTasks.get();

                                // Collect all feedback results
                                List<String> allFeedback = new ArrayList<>();
                                for (CompletableFuture<String> task : feedbackTasks) {
                                        allFeedback.add(task.get());
                                }

                                log.info("Collected {} pieces of feedback for session: {}", allFeedback.size(),
                                                sessionId);
                                return allFeedback;

                        } catch (Exception e) {
                                log.error("Error collecting comprehensive feedback for session: " + sessionId, e);
                                throw new RuntimeException("Failed to collect feedback", e);
                        }
                });
        }

        private CompletableFuture<String> processAIHumanApproval(String finalPresentation, String developmentJourney,
                        String sessionId) {
                return CompletableFuture.supplyAsync(() -> {
                        try {
                                log.info("Starting AI-simulated human approval process for session: {}", sessionId);

                                // Executive review
                                String executiveReview = invokeSkillByReceptionist(humanAgentAgentName,
                                                "executive-review",
                                                List.of(finalPresentation)).get();

                                // Final approval decision
                                String approvalDecision = invokeSkillByReceptionist(humanAgentAgentName,
                                                "final-approval-decision",
                                                List.of(executiveReview, developmentJourney)).get();

                                if (approvalDecision.contains("APPROVE")) {
                                        // Strategic implementation guidance
                                        String strategicGuidance = invokeSkillByReceptionist(humanAgentAgentName,
                                                        "strategic-implementation-guidance",
                                                        List.of(approvalDecision)).get();

                                        log.info("Product APPROVED for session: {}", sessionId);
                                        return "PRODUCT_APPROVED:\n\n" + approvalDecision
                                                        + "\n\nSTRATEGIC_GUIDANCE:\n\n"
                                                        + strategicGuidance;
                                } else {
                                        log.info("Product REJECTED for session: {}", sessionId);
                                        return "PRODUCT_REJECTED:\n\n" + approvalDecision;
                                }

                        } catch (Exception e) {
                                log.error("Error in AI human approval process for session: " + sessionId, e);
                                return "APPROVAL_PROCESS_ERROR: Failed during approval - " + e.getMessage();
                        }
                });
        }

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

        public record A2AReceptionistSkill(
                        String id,
                        String agentName,
                        String agentUrl,
                        String skillDescription,
                        Double confidence,
                        String skilId) {
        }
}
