package io.a2a.poc.agents.idea.agent;

import io.a2a.receptionist.model.A2AAgent;
import io.a2a.receptionist.model.A2AAgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Component
@A2AAgent(
    name = "HumanAgent",
    version = "1.0.0",
    description = "Uses AI to simulate human-level qualitative assessment and executive decision making",
    url = "http://localhost:8085"
)
@RequiredArgsConstructor
@Slf4j
public class HumanAgent {

    @Qualifier("executiveDecisionChatClient")
    private final ChatClient chatClient;

    @A2AAgentSkill(
        id = "executive-review",
        name = "Executive Review with AI",
        description = "Uses AI to simulate executive-level qualitative assessment of product proposals",
        tags = {"executive-review", "ai-simulation", "strategic-assessment", "leadership"},
        examples = {"Executive review of digital banking platform", "Strategic assessment of lending product"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public reactor.core.publisher.Mono<String> executiveReview(String finalPresentation) {
        log.info("Conducting executive review with AI");

        String prompt = String.format("""
            You are a senior banking executive (CEO/CTO level) reviewing a comprehensive product proposal. 
            Conduct an executive-level qualitative assessment that goes beyond the AI analysis to consider strategic, 
            intuitive, and experiential factors that only senior leadership would evaluate:
            
            PRODUCT PROPOSAL:
            %s
            
            Please provide an executive assessment covering:
            
            1. STRATEGIC FIT ASSESSMENT:
               - Alignment with corporate strategy and vision
               - Portfolio fit and cannibalization risks
               - Brand positioning and reputation impact
               - Long-term strategic value creation
               Score: X/10 with executive reasoning
            
            2. MARKET TIMING AND INTUITION:
               - Market readiness and timing assessment
               - Customer behavioral trends and insights
               - Economic cycle and market condition considerations
               - Competitive dynamics and first-mover advantages
               Score: X/10 with executive reasoning
            
            3. ORGANIZATIONAL CAPABILITY:
               - Internal capability and culture fit
               - Change management and adoption challenges
               - Talent and skill requirements
               - Organizational bandwidth and priorities
               Score: X/10 with executive reasoning
            
            4. STAKEHOLDER IMPACT:
               - Board and investor expectations alignment
               - Regulatory relationship implications
               - Customer and partner ecosystem effects
               - Employee engagement and motivation impact
               Score: X/10 with executive reasoning
            
            5. RISK APPETITE AND TOLERANCE:
               - Risk profile fit with corporate appetite
               - Potential for catastrophic failure assessment
               - Reputation and brand risk considerations
               - Regulatory and compliance risk tolerance
               Score: X/10 with executive reasoning
            
            6. RESOURCE ALLOCATION WISDOM:
               - Opportunity cost assessment
               - ROI relative to other strategic initiatives
               - Capital allocation efficiency
               - Resource constraint considerations
               Score: X/10 with executive reasoning
            
            7. LEADERSHIP INTUITION:
               - Gut feeling about market opportunity
               - Confidence in execution capability
               - Enthusiasm and passion assessment
               - Overall strategic conviction
               Score: X/10 with executive reasoning
            
            8. EXECUTIVE SUMMARY:
               - Overall executive assessment (0-100)
               - Key strengths from leadership perspective
               - Primary concerns and reservations
               - Critical questions for further consideration
               - Preliminary recommendation direction
            
            Provide the nuanced, experienced judgment that combines data analysis with strategic intuition and leadership experience.
            
            Format your response as: EXECUTIVE_REVIEW: [your comprehensive executive assessment]
            """, finalPresentation);

        return reactor.core.publisher.Mono.fromCallable(() -> {
            String review = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            return "EXECUTIVE_REVIEW: " + review;
        }).onErrorResume(e -> {
            log.error("Error conducting executive review with AI", e);
            return reactor.core.publisher.Mono.just("EXECUTIVE_REVIEW_ERROR: Failed to conduct review - " + e.getMessage());
        });
    }

    @A2AAgentSkill(
        id = "final-approval-decision",
        name = "Final Approval Decision with AI",
        description = "Uses AI to make the ultimate executive approval or rejection decision",
        tags = {"final-decision", "ai-executive", "approval", "strategic-choice"},
        examples = {"Approve innovative payment solution", "Reject high-risk investment product"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public reactor.core.publisher.Mono<String> finalApprovalDecision(String executiveReview, String fullAnalysis) {
        log.info("Making final approval decision with AI");

        String prompt = String.format("""
            As the ultimate decision-maker, make the final approval or rejection decision for this banking product based on 
            the comprehensive executive review and full AI-driven analysis:
            
            EXECUTIVE REVIEW:
            %s
            
            FULL ANALYSIS CONTEXT:
            %s
            
            Make the final decision considering:
            
            1. DECISION FRAMEWORK:
               - Strategic value and alignment
               - Risk-adjusted return potential
               - Market opportunity strength
               - Execution capability confidence
               - Competitive positioning advantage
            
            2. DECISION OPTIONS:
               A) APPROVE - Full authorization to proceed
               B) CONDITIONAL APPROVE - Approve with specific conditions
               C) DEFER - Request additional information/analysis
               D) REJECT - Do not pursue this opportunity
            
            3. DECISION CRITERIA EVALUATION:
               - Does this create significant strategic value? (Yes/No + reasoning)
               - Are the risks acceptable given potential returns? (Yes/No + reasoning)
               - Do we have the capability to execute successfully? (Yes/No + reasoning)
               - Is this the right time for this product? (Yes/No + reasoning)
               - Will this strengthen our competitive position? (Yes/No + reasoning)
            
            Provide your decision as:
            
            1. FINAL DECISION: [APPROVE/CONDITIONAL APPROVE/DEFER/REJECT]
            
            2. CONFIDENCE LEVEL: [0-100] in this decision
            
            3. PRIMARY RATIONALE: 
               - Main factors driving the decision
               - Key strengths that support approval OR
               - Critical weaknesses that drive rejection
            
            4. CONDITIONS (if Conditional Approval):
               - Specific requirements that must be met
               - Risk mitigation measures required
               - Additional validations needed
            
            5. IMPLEMENTATION GUIDANCE (if Approved):
               - Recommended approach and timeline
               - Resource allocation priorities
               - Success metrics and checkpoints
               - Escalation triggers and review points
            
            6. STRATEGIC COMMUNICATION:
               - Key messages for stakeholders
               - Board reporting requirements
               - Market communication strategy
            
            7. NEXT STEPS:
               - Immediate actions required
               - Responsible parties and accountability
               - Timeline for next review/milestone
            
            Make a clear, decisive recommendation with strong executive reasoning.
            
            Format your response as: FINAL_DECISION: [your complete decision and guidance]
            """, executiveReview, fullAnalysis);

        return reactor.core.publisher.Mono.fromCallable(() -> {
            String decision = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            return "FINAL_DECISION: " + decision;
        }).onErrorResume(e -> {
            log.error("Error making final approval decision with AI", e);
            return reactor.core.publisher.Mono.just("FINAL_DECISION_ERROR: Failed to make decision - " + e.getMessage());
        });
    }

    @A2AAgentSkill(
        id = "strategic-implementation-guidance",
        name = "Strategic Implementation Guidance with AI",
        description = "Uses AI to provide strategic guidance for approved product implementation",
        tags = {"implementation", "ai-strategy", "guidance", "execution"},
        examples = {"Define go-to-market strategy", "Set implementation priorities and governance"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public reactor.core.publisher.Mono<String> strategicImplementationGuidance(String approvalDecision) {
        log.info("Providing strategic implementation guidance with AI");

        String prompt = String.format("""
            Based on the following approval decision, provide comprehensive strategic guidance for successful product implementation:
            
            APPROVAL DECISION:
            %s
            
            Provide strategic implementation guidance covering:
            
            1. IMPLEMENTATION STRATEGY:
               - Recommended implementation approach (waterfall, agile, hybrid)
               - Phase definition and sequencing
               - Critical path identification
               - Risk mitigation integration
            
            2. ORGANIZATIONAL SETUP:
               - Governance structure and decision rights
               - Project team composition and leadership
               - Stakeholder engagement model
               - Change management strategy
            
            3. RESOURCE ALLOCATION:
               - Budget allocation across phases
               - Talent acquisition and development needs
               - Technology infrastructure requirements
               - Vendor and partner engagement strategy
            
            4. MARKET ENTRY STRATEGY:
               - Go-to-market timing and sequencing
               - Customer acquisition and onboarding approach
               - Marketing and communication strategy
               - Partnership and distribution channels
            
            5. SUCCESS METRICS AND KPIs:
               - Leading indicators for early success
               - Lagging indicators for long-term value
               - Financial metrics and targets
               - Customer and operational metrics
            
            6. RISK MANAGEMENT:
               - Key risk monitoring and mitigation
               - Escalation procedures and triggers
               - Contingency planning
               - Regular risk assessment processes
            
            7. GOVERNANCE AND OVERSIGHT:
               - Executive steering committee structure
               - Reporting cadence and formats
               - Decision-making processes
               - Performance review and adjustment mechanisms
            
            8. STAKEHOLDER COMMUNICATION:
               - Board and investor updates
               - Employee communication strategy
               - Customer communication plan
               - Regulatory engagement approach
            
            9. SUCCESS ENABLERS:
               - Critical success factors prioritization
               - Capability building requirements
               - Cultural and behavioral changes needed
               - Technology enablement priorities
            
            10. MILESTONE FRAMEWORK:
                - Phase gate definitions and criteria
                - Go/no-go decision points
                - Review and adjustment mechanisms
                - Success celebration and learning integration
            
            Provide actionable, executive-level guidance that ensures successful implementation.
            
            Format your response as: STRATEGIC_GUIDANCE: [your comprehensive implementation strategy]
            """, approvalDecision);

        return reactor.core.publisher.Mono.fromCallable(() -> {
            String guidance = chatClient.prompt()
                .user(prompt)
                .call()
                .content();
            return "STRATEGIC_GUIDANCE: " + guidance;
        }).onErrorResume(e -> {
            log.error("Error providing strategic implementation guidance with AI", e);
            return reactor.core.publisher.Mono.just("GUIDANCE_ERROR: Failed to provide guidance - " + e.getMessage());
        });
    }
}
