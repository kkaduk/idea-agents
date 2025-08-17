package io.a2a.poc.agents.idea.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.a2a.poc.agents.idea.service.IdeaProductWorkflowOrchestrator.A2AReceptionistSkill;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Build the planner/dispatcher prompt by injecting runtime values into the
 * template.
 * Resulting string is ready to pass to your LLM as a single chat message.
 */
public class PlannerPromptBuilder {

    // === Prompt template with placeholders ===
    private static final String TEMPLATE = """
            You are a planner that selects the best agent skills to fulfill a user task.

            You must ONLY use the skills provided in {{SKILLS_JSON}}.
            When in doubt, be conservative: prefer fewer, high-confidence skills.
            Output valid JSON only that conforms to the following schema:

            {
              "taskId": "string",
              "executionMode": "sequential | parallel",
              "selectedSkills": [
                {
                  "stepId": "string",
                  "agentName": "string",
                  "agentUrl": "string",
                  "skillId": "string",
                  "confidence": 0.0,
                  "priority": 1,
                  "timeoutSec": 0,
                  "retries": { "maxAttempts": 0, "backoffSec": 0 },
                  "input": { },
                  "dependsOn": ["stepId-1", "stepId-2"]
                }
              ],
              "reason": "string (optional; empty if skills selected)"
            }

            Rules:
            1. Do not invent skills, agents, or URLs. Use only entries from the provided skills list.
            2. Respect the minimum confidence threshold: {{MIN_CONFIDENCE_FLOAT}}.
            3. Normalize the skill identifier: in the catalog it may be "skilId"; expose it as "skillId" in the output.
            4. If multiple skills are needed, create an execution plan with "executionMode" = "sequential" or "parallel".
            5. For sequential chains, use "dependsOn" to reference prior steps by "stepId".
            6. Extract minimal, structured "input" arguments required by the chosen skill(s) from the task description.
            7. Set "priority" (1–5, 1 = highest) and a reasonable "timeoutSec" (default 120 if unspecified).
            8. Include a retry policy: { "maxAttempts": 2, "backoffSec": 3 } unless otherwise stated.
            9. If no suitable skill exists, return an empty "selectedSkills" array and provide a non-empty "reason".
            10. Return JSON only. Do not include any explanations or extra text.

            User task:
            {{USER_TASK_JSON}}

            Skill catalog:
            {{SKILLS_JSON}}

            Minimum confidence:
            {{MIN_CONFIDENCE_FLOAT}}

            Return JSON only.
            """;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL)
            .enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Build the final prompt by replacing placeholders with serialized JSON.
     */
    public static String build(UserTask userTask, List<A2AReceptionistSkill> skillsCatalog, double minConfidence) {
        Objects.requireNonNull(userTask, "userTask");
        Objects.requireNonNull(skillsCatalog, "skillsCatalog");

        try {
            String skillsJson = MAPPER.writeValueAsString(skillsCatalog);
            String taskJson = MAPPER.writeValueAsString(userTask);
            String threshold = Double.toString(minConfidence);

            return TEMPLATE
                    .replace("{{SKILLS_JSON}}", skillsJson)
                    .replace("{{USER_TASK_JSON}}", taskJson)
                    .replace("{{MIN_CONFIDENCE_FLOAT}}", threshold);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render planner prompt", e);
        }
    }

    // === DTOs you can adapt to your domain ===
    public record UserTask(
            String title,
            String description,
            Map<String, Object> constraints) {
    }

    // public record AgentSkill(
    //         String id,
    //         String agentName,
    //         String agentUrl,
    //         String skillDescription,
    //         Double confidence,
    //         String skilId // note: source field uses single 'l'
    // ) {
    // }


    // === Example usage (plain Java) ===
    public static void main(String[] args) {
        List<A2AReceptionistSkill> catalog = List.of(
                new A2AReceptionistSkill("analyze-legislation", "IdeaCreatorAgent", "http://localhost:8081",
                        "Uses AI to analyze government legislation and identify banking opportunities",
                        0.8305, "analyze-legislation"),
                new A2AReceptionistSkill("create-product-idea", "IdeaCreatorAgent", "http://localhost:8081",
                        "Uses AI to formulate detailed banking product propositions based on opportunities",
                        0.8305, "create-product-idea"),
                new A2AReceptionistSkill("comprehensive-product-critique", "IdeaCriticAgent", "http://localhost:8083",
                        "Uses AI to provide thorough critical analysis of banking product ideas",
                        0.610775, "comprehensive-product-critique"));

        UserTask task = new UserTask(
                "Draft a new retail savings product from fresh legislation",
                "Analyze the new consumer credit amendment 2025 and propose a product idea suitable for millennials.",
                Map.of("country", "PL", "segment", "millennials"));

        String prompt = PlannerPromptBuilder.build(task, catalog, 0.7);
        System.out.println(prompt);
    }
}