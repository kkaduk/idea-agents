package io.a2a.poc.agents.idea.util;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import io.a2a.poc.agents.idea.service.ProductIdeaWorkflowOrchestrator.A2AReceptionistSkill;

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
                              "input": { "key": "value" },   // MUST NOT be empty
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
                           - "input" must always contain at least one non-empty key/value pair.
                           - Do not allow an empty object `{}`.
                        7. Set "priority" (1-5, 1 = highest) and a reasonable "timeoutSec" (default 120 if unspecified).
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

        public record UserTask(
                        String title,
                        String description,
                        Map<String, Object> constraints) {
        }

        private static final String TEMPLATE_SKILLS = """
                                You are an expert metadata extractor for opportunity detection and agent skill routing.
                                Your goal is to read the provided document text and output only a compact JSON object:
                                {
                                   "title" : "....",
                                   "description" : "...",
                                   "keywords": ["..."],
                                   "skills": ["..."],
                                   "requiredTags" : ["..."]
                                   "
                                }

                                Output rules
                                Output JSON only (no prose).
                                keywords → 6-14 concise, lowercase phrases (deduplicated). Prefer: jurisdiction, sector, legal objects, binding provisions, dates, numbers, affected parties, operational constraints.
                                requiredTags → 8-15 routing tags in key:value form, chosen from the controlled vocabulary below. Include at least one skill:* tag.
                                Controlled vocabulary (aligns with your agents)
                                Context & scope
                                jurisdiction:<iso2> (e.g., jurisdiction:pl)
                                sector:education|finance|health|energy|public-sector|local-gov
                                instrument:act|regulation|resolution|citizens-initiative
                                legal-status:draft|pending|in-force|repeal|amendment
                                timeline:<yyyy-mm-dd> (effective/onset date)
                                stakeholder:schools|parents|students|teachers|local-gov|moe
                                impact:budget|compliance|operational|reputation
                                opportunity:regulatory|market|partnership|funding
                                risk:legal-challenge|political|public-perception|supply|data
                                priority (include when time-sensitive or high-impact)

                                Agent skills (choose any that apply)
                                skill:monitor-legislation — watch docket, updates, deadlines
                                skill:policy-diff — compare current vs proposed text, extract deltas
                                skill:analyze-legislation — summarize obligations, constraints, dependencies
                                skill:impact-assessment — quantify operational/budget/compliance impact
                                skill:stakeholder-mapping — map actors, rights, duties, decision points
                                skill:compliance-check — translate provisions to checklists & controls
                                skill:opportunity-mining — detect product/service opportunities
                                skill:create-product-idea — craft product concept canvases from opportunities
                                skill:market-sizing — estimate TAM/SAM/SOM from policy scope
                                skill:risk-scoring — legal/political/ops risk with likelihood×impact
                                skill:go-to-market — channels, ICPs, procurement paths (public sector)
                                skill:data-requirements — identify datasets, integration points, KPIs
                                skill:edu-scheduling-optimizer — scheduling/grouping under legal constraints
                                skill:teacher-workforce-planner — staffing, recruitment, training projections
                                skill:budget-estimation — quantify cost items (e.g., extra FTE, capex/opex)

                                Description - provide a summary of the product idea based on the document

                                Quality checks
                                Use ISO dates (YYYY-MM-DD).
                                Normalize numbers in keywords (e.g., 141.7 mln pln).
                                Prefer multiword domain concepts over single tokens.
                                Omit unknown tags; don’t guess.
                                DOCUMENT
                                {{document_text}}

                        """;

        public static String buildSkillsPrompt(String billSummary) {
                Objects.requireNonNull(billSummary, "billSummary");

                try {

                        return TEMPLATE_SKILLS
                                        .replace("{{document_text}}", billSummary);

                } catch (Exception e) {
                        throw new IllegalStateException("Failed to render planner prompt", e);
                }
        }

        // === Example usage (plain Java) ===
        public static void main(String[] args) {
                List<A2AReceptionistSkill> catalog = List.of(
                                new A2AReceptionistSkill("analyze-legislation", "IdeaCreatorAgent",
                                                "http://localhost:8081",
                                                "Uses AI to analyze government legislation and identify banking opportunities",
                                                0.8305, "analyze-legislation"),
                                new A2AReceptionistSkill("create-product-idea", "IdeaCreatorAgent",
                                                "http://localhost:8081",
                                                "Uses AI to formulate detailed banking product propositions based on opportunities",
                                                0.8305, "create-product-idea"),
                                new A2AReceptionistSkill("comprehensive-product-critique", "IdeaCriticAgent",
                                                "http://localhost:8083",
                                                "Uses AI to provide thorough critical analysis of banking product ideas",
                                                0.610775, "comprehensive-product-critique"));

                UserTask task = new UserTask(
                                "Draft a new retail savings product from fresh legislation",
                                "Analyze the new consumer credit amendment 2025 and propose a product idea suitable for millennials.",
                                Map.of("country", "PL", "segment", "millennials"));

                String prompt = PlannerPromptBuilder.build(task, catalog, 0.7);
                System.out.println(prompt);

                String documentSummary = """
                                The document is a citizens’ legislative initiative submitted to the Polish Sejm in June 2025. It proposes amendments to the Education System Act (1991) and the Education Law (2016) concerning the teaching of religion and ethics in schools. The core change is the introduction of mandatory religion or ethics classes (two hours per week) in preschools, primary and secondary schools, and art schools (excluding adult schools). Parents or adult students will be required to choose between religion and ethics, with the choice declared annually.
                                The proposal defines organizational rules (minimum of 7 students per class, possibility of interschool groups, group size limits), teacher qualifications (appointed by religious authorities in consultation with the Ministry of Education), and evaluation principles (grades count toward GPA and promotion). It also includes provisions on retreats (rekolekcje), the role of religion teachers in the school community, supervision and inspections, and the presence of religious symbols such as the cross.
                                The justification stresses the importance of moral and ethical education for youth development, invoking constitutional guarantees (Art. 53), cultural heritage, and international comparisons (Austria, Belgium, Lithuania, etc.). It argues for stabilizing these provisions at the statutory level rather than ministerial regulations to prevent ad hoc policy changes. The financial impact is estimated at about 141.7 million PLN annually, mainly due to the need for additional ethics teachers. The law is intended to enter into force on 1 September 2025.
                                """;
                String skillsPrompt = buildSkillsPrompt(documentSummary);
                System.out.println("====================");
                System.out.println(skillsPrompt);

        }
}