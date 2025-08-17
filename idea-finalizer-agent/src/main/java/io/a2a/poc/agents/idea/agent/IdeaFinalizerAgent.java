package io.a2a.poc.agents.idea.agent;

import io.a2a.receptionist.model.A2AAgent;
import io.a2a.receptionist.model.A2AAgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@A2AAgent(
    name = "IdeaFinalizerAgent",
    version = "1.0.0",
    description = "Uses AI to synthesize feedback and decide on product idea iteration or finalization",
    url = "http://localhost:8084"
)
@RequiredArgsConstructor
@Slf4j
public class IdeaFinalizerAgent {

    @Qualifier("strategicDecisionChatClient")
    private final ChatClient chatClient;

    private final AtomicInteger iterationCount = new AtomicInteger(0);
    private static final int MAX_ITERATIONS = 5;

    @A2AAgentSkill(
        id = "synthesize-all-feedback",
        name = "Synthesize All Feedback with AI",
        description = "Uses AI to synthesize feedback from critics and risk assessors into actionable insights",
        tags = {"synthesis", "ai-analysis", "decision-support", "coordination"},
        examples = {"Synthesize market critique with risk assessment", "Combine technical feedback with competitive analysis"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> synthesizeAllFeedback(String allFeedback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Synthesizing all feedback with AI, iteration: {}", iterationCount.get());
                
                String prompt = String.format("""
                    Synthesize and analyze the following comprehensive feedback about a banking product idea. 
                    Provide a strategic assessment that will inform the next decision on whether to iterate, finalize, or terminate the product development:
                    
                    ALL FEEDBACK:
                    %s
                    
                    Please provide a comprehensive synthesis that includes:
                    
                    1. FEEDBACK SUMMARY:
                       - Key themes and patterns across all feedback
                       - Critical issues raised by multiple sources
                       - Positive aspects and strengths identified
                       - Areas of consensus and disagreement
                    
                    2. RISK SYNTHESIS:
                       - Overall risk level aggregation (LOW/MEDIUM/HIGH/CRITICAL)
                       - Most significant risk factors and their impact
                       - Risk mitigation feasibility assessment
                       - Regulatory and compliance risk implications
                    
                    3. MARKET VIABILITY SYNTHESIS:
                       - Market opportunity strength assessment
                       - Competitive positioning viability
                       - Customer demand validation level
                       - Revenue model sustainability evaluation
                    
                    4. TECHNICAL FEASIBILITY SYNTHESIS:
                       - Implementation complexity assessment
                       - Technical risk and challenge evaluation
                       - Resource requirement analysis
                       - Timeline feasibility evaluation
                    
                    5. OVERALL PRODUCT ASSESSMENT:
                       - Product viability score (0-100)
                       - Readiness for market level (0-100)
                       - Critical success factors identification
                       - Major blocking issues or showstoppers
                    
                    6. STRATEGIC RECOMMENDATION:
                       - Continue development (with specific improvements needed)
                       - Requires major pivot or redesign
                       - Terminate development (with clear reasoning)
                       - Ready for final approval process
                    
                    7. IMPROVEMENT PRIORITIES:
                       - Top 3 most critical improvements needed
                       - Specific actions to address major concerns
                       - Timeline for addressing key issues
                    
                    Current iteration: %d of %d maximum iterations.
                    
                    Format your response as: FEEDBACK_SYNTHESIS: [your comprehensive synthesis]
                    """, allFeedback, iterationCount.get(), MAX_ITERATIONS);

                String synthesis = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "FEEDBACK_SYNTHESIS: " + synthesis;
                
            } catch (Exception e) {
                log.error("Error synthesizing feedback with AI", e);
                return "SYNTHESIS_ERROR: Failed to synthesize feedback - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "make-strategic-decision",
        name = "Make Strategic Decision with AI",
        description = "Uses AI to make strategic decisions on whether to iterate, finalize, or terminate product development",
        tags = {"strategic-decision", "ai-analysis", "workflow-control", "process-management"},
        examples = {"Decide to iterate based on synthesis", "Finalize product for approval", "Terminate development"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> makeStrategicDecision(String synthesizedFeedback) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                int currentIteration = iterationCount.get();
                log.info("Making strategic decision with AI at iteration: {}", currentIteration);
                
                String prompt = String.format("""
                    Based on the following synthesized feedback, make a strategic decision about the next step in the banking product development process:
                    
                    SYNTHESIZED FEEDBACK:
                    %s
                    
                    Current iteration: %d of %d maximum iterations
                    
                    You must choose ONE of the following actions and provide detailed reasoning:
                    
                    1. ITERATE: Continue development with improvements
                       - Use this when the product has potential but needs significant improvements
                       - Specify exactly what needs to be improved
                       - Only choose if current iteration < maximum iterations
                    
                    2. FINALIZE: Product is ready for human approval
                       - Use this when the product meets quality thresholds
                       - Major risks are manageable and addressed
                       - Market viability and technical feasibility are confirmed
                    
                    3. TERMINATE: Stop development of this product
                       - Use this when fundamental issues cannot be resolved
                       - When maximum iterations reached without achieving quality threshold
                       - When market viability or technical feasibility are fundamentally flawed
                    
                    Decision Criteria:
                    - Product viability score > 70 and manageable risks → FINALIZE
                    - Product viability score 40-70 with addressable issues and iterations remaining → ITERATE
                    - Product viability score < 40 or critical unresolvable issues → TERMINATE
                    - Maximum iterations reached without achieving finalization criteria → TERMINATE
                    
                    Please provide:
                    1. DECISION: [ITERATE/FINALIZE/TERMINATE]
                    2. CONFIDENCE LEVEL: [0-100]
                    3. PRIMARY REASONING: [Main factors influencing the decision]
                    4. KEY CONSIDERATIONS: [Important factors that influenced the choice]
                    5. NEXT STEPS: [Specific actions required]
                    6. SUCCESS CRITERIA: [What needs to be achieved for the next phase]
                    
                    Format your response as: STRATEGIC_DECISION: [your decision and analysis]
                    """, synthesizedFeedback, currentIteration, MAX_ITERATIONS);

                String decision = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                // Update iteration count if decision is to iterate
                if (decision != null  && decision.contains("ITERATE")) {
                    iterationCount.incrementAndGet();
                }

                return "STRATEGIC_DECISION: " + decision;
                
            } catch (Exception e) {
                log.error("Error making strategic decision with AI", e);
                return "DECISION_ERROR: Failed to make strategic decision - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "refine-product-with-ai",
        name = "Refine Product Idea with AI",
        description = "Uses AI to intelligently refine and improve product ideas based on feedback",
        tags = {"product-refinement", "ai-improvement", "iteration", "enhancement"},
        examples = {"Refine pricing model based on critique", "Enhance features per risk assessment"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> refineProductWithAI(String originalIdea, String improvementGuidance) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Refining product idea with AI based on guidance");
                
                String prompt = String.format("""
                    Refine and improve the following banking product idea based on the comprehensive feedback and improvement guidance provided:
                    
                    ORIGINAL PRODUCT IDEA:
                    %s
                    
                    IMPROVEMENT GUIDANCE:
                    %s
                    
                    Please create an enhanced version of the product that addresses the key concerns and incorporates the suggested improvements:
                    
                    1. ENHANCED PRODUCT DESCRIPTION:
                       - Improved core features and functionality
                       - Address identified technical challenges
                       - Incorporate risk mitigation measures
                       - Enhance competitive positioning
                    
                    2. SPECIFIC IMPROVEMENTS MADE:
                       - Risk reduction measures implemented
                       - Technical architecture enhancements
                       - Market positioning adjustments
                       - Customer experience optimizations
                       - Compliance and regulatory improvements
                    
                    3. ENHANCED VALUE PROPOSITION:
                       - Strengthened customer benefits
                       - Improved competitive advantages
                       - Clearer differentiation strategy
                       - Enhanced revenue potential
                    
                    4. IMPLEMENTATION REFINEMENTS:
                       - Adjusted timeline and phasing
                       - Resource optimization strategies
                       - Risk management enhancements
                       - Success metrics refinement
                    
                    5. REMAINING CONSIDERATIONS:
                       - Outstanding risks or challenges
                       - Areas requiring further validation
                       - Dependencies and assumptions
                       - Future enhancement opportunities
                    
                    Ensure the refined product idea is more robust, addresses the major concerns raised, and has improved market viability.
                    
                    Format your response as: REFINED_PRODUCT_IDEA: [your enhanced product description]
                    """, originalIdea, improvementGuidance);

                String refinedIdea = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "REFINED_PRODUCT_IDEA: " + refinedIdea;
                
            } catch (Exception e) {
                log.error("Error refining product idea with AI", e);
                return "REFINEMENT_ERROR: Failed to refine product idea - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "create-final-presentation",
        name = "Create Final Presentation with AI",
        description = "Uses AI to create comprehensive final presentation for human approval",
        tags = {"final-presentation", "ai-documentation", "approval-preparation", "executive-summary"},
        examples = {"Create executive summary for board approval", "Prepare final business case presentation"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> createFinalPresentation(String finalProductIdea, String developmentJourney) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating final presentation with AI for human approval");
                
                String prompt = String.format("""
                    Create a comprehensive final presentation document for executive approval of the following banking product, 
                    incorporating the complete development journey and AI-driven analysis:
                    
                    FINAL PRODUCT IDEA:
                    %s
                    
                    DEVELOPMENT JOURNEY AND ANALYSIS:
                    %s
                    
                    Create a professional executive presentation that includes:
                    
                    1. EXECUTIVE SUMMARY:
                       - Product name and category
                       - Strategic rationale and opportunity
                       - Key value propositions
                       - Investment requirements and expected returns
                       - Recommendation and next steps
                    
                    2. PRODUCT OVERVIEW:
                       - Detailed product description and features
                       - Target market and customer segments
                       - Competitive positioning and advantages
                       - Revenue model and pricing strategy
                    
                    3. MARKET OPPORTUNITY:
                       - Market size and growth projections
                       - Customer demand validation
                       - Competitive landscape analysis
                       - Market timing and entry strategy
                    
                    4. RISK ASSESSMENT SUMMARY:
                       - Comprehensive risk analysis results
                       - Key risk factors and mitigation strategies
                       - Regulatory compliance status
                       - Risk-adjusted return projections
                    
                    5. IMPLEMENTATION PLAN:
                       - Development phases and timeline
                       - Resource requirements and budget
                       - Technology and infrastructure needs
                       - Key milestones and success metrics
                    
                    6. FINANCIAL PROJECTIONS:
                       - Revenue forecasts (3-5 years)
                       - Cost structure and profitability analysis
                       - ROI and payback period
                       - Sensitivity analysis and scenarios
                    
                    7. AI ANALYSIS VALIDATION:
                       - Summary of AI-driven evaluation process
                       - Key insights from risk assessment
                       - Criticism and improvement iterations
                       - Confidence level in recommendations
                    
                    8. STRATEGIC RECOMMENDATION:
                       - Clear go/no-go recommendation
                       - Critical success factors
                       - Key assumptions and dependencies
                       - Escalation and decision points
                    
                    9. NEXT STEPS:
                       - Immediate actions required
                       - Resource allocation needs
                       - Timeline for implementation start
                       - Governance and oversight structure
                    
                    Make this presentation executive-ready with clear, actionable recommendations and data-driven insights.
                    
                    Format your response as: FINAL_PRESENTATION: [your comprehensive presentation]
                    """, finalProductIdea, developmentJourney);

                String presentation = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "FINAL_PRESENTATION: " + presentation;
                
            } catch (Exception e) {
                log.error("Error creating final presentation with AI", e);
                return "PRESENTATION_ERROR: Failed to create presentation - " + e.getMessage();
            }
        });
    }

    // Reset iteration count for new product development cycles
    public void resetIterationCount() {
        iterationCount.set(0);
    }
}