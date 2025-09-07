package io.a2a.poc.agents.idea.agent;

import io.a2a.receptionist.model.A2AAgent;
import io.a2a.receptionist.model.A2AAgentSkill;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Component
@A2AAgent(
    name = "IdeaCriticAgent",
    version = "1.0.0",
    description = "Uses AI to provide critical analysis and feedback on banking product ideas",
    url = "http://localhost:8083"
)
@Slf4j
public class IdeaCriticAgent {
    private final ChatClient chatClient;

    public IdeaCriticAgent(@Qualifier("criticAnalysisChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @A2AAgentSkill(
        id = "comprehensive-product-critique",
        name = "Comprehensive Product Critique with AI",
        description = "Uses AI to provide thorough critical analysis of banking product ideas",
        tags = {"product-criticism", "ai-analysis", "comprehensive", "evaluation"},
        examples = {"Critique digital wallet business model", "Analyze competitive positioning of lending product"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public java.util.concurrent.CompletableFuture<String> comprehensiveProductCritique(String productIdea) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing comprehensive product critique with AI for: {}", 
                        productIdea.substring(0, Math.min(100, productIdea.length())));
                
                String prompt = String.format("""
                    Provide a comprehensive critical analysis of the following banking product idea. Be thorough, objective, and constructive in identifying potential weaknesses, challenges, and areas for improvement:
                    
                    PRODUCT IDEA:
                    %s
                    
                    Please analyze and critique across these dimensions:
                    
                    1. MARKET VIABILITY:
                       - Target market size and accessibility
                       - Customer demand validation and evidence
                       - Market timing and economic conditions
                       - Competitive landscape and differentiation challenges
                       - Market entry barriers and customer acquisition costs
                       Score: X/10 with detailed justification
                    
                    2. TECHNICAL FEASIBILITY:
                       - Implementation complexity and technical risks
                       - Integration challenges with existing systems
                       - Scalability and performance concerns
                       - Security and data protection requirements
                       - Technology stack appropriateness and future-proofing
                       Score: X/10 with detailed justification
                    
                    3. BUSINESS MODEL VIABILITY:
                       - Revenue model sustainability and profitability
                       - Cost structure and margin analysis
                       - Customer lifetime value and acquisition economics
                       - Pricing strategy competitiveness
                       - Long-term financial sustainability
                       Score: X/10 with detailed justification
                    
                    4. CUSTOMER EXPERIENCE:
                       - User journey complexity and friction points
                       - Customer onboarding and adoption challenges
                       - User interface and accessibility considerations
                       - Customer support and service requirements
                       - Customer satisfaction and retention risks
                       Score: X/10 with detailed justification
                    
                    5. COMPETITIVE POSITIONING:
                       - Unique value proposition strength
                       - Competitive advantages sustainability
                       - Threat from existing players and new entrants
                       - Market positioning clarity and differentiation
                       - Brand and marketing requirements
                       Score: X/10 with detailed justification
                    
                    6. OPERATIONAL CONSIDERATIONS:
                       - Resource requirements and availability
                       - Organizational capabilities and skill gaps
                       - Process complexity and operational risks
                       - Vendor dependencies and third-party risks
                       - Regulatory and compliance operational burden
                       Score: X/10 with detailed justification
                    
                    7. OVERALL ASSESSMENT:
                       - Overall viability score (0-100)
                       - Critical success factors and key assumptions
                       - Major risks and potential failure points
                       - Recommended improvements and modifications
                       - Go/No-Go recommendation with reasoning
                    
                    Be critical but constructive. Identify specific weaknesses and provide actionable recommendations.
                    Format your response as: COMPREHENSIVE_CRITIQUE: [your detailed analysis]
                    """, productIdea);

                String critique = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "COMPREHENSIVE_CRITIQUE: " + critique;
            } catch (Exception e) {
                log.error("Error performing comprehensive critique with AI", e);
                return "CRITIQUE_ERROR: Failed to analyze product - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "competitive-analysis",
        name = "Competitive Analysis with AI",
        description = "Uses AI to analyze competitive landscape and positioning challenges",
        tags = {"competitive-analysis", "ai-analysis", "market-research", "positioning"},
        examples = {"Analyze competition in digital payments", "Evaluate competitive threats for new lending product"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public java.util.concurrent.CompletableFuture<String> competitiveAnalysis(String productIdea) {
        return java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing competitive analysis with AI for: {}", 
                        productIdea.substring(0, Math.min(100, productIdea.length())));
                
                String prompt = String.format("""
                    Conduct a comprehensive competitive analysis for the following banking product idea:
                    
                    PRODUCT IDEA:
                    %s
                    
                    Please analyze:
                    
                    1. DIRECT COMPETITORS:
                       - Identify 3-5 direct competitors offering similar products
                       - Analyze their market position, strengths, and weaknesses
                       - Compare features, pricing, and customer experience
                       - Assess their market share and customer base
                    
                    2. INDIRECT COMPETITORS:
                       - Identify alternative solutions customers might choose
                       - Analyze fintech disruptors and technology companies
                       - Consider non-traditional competitors (big tech, etc.)
                    
                    3. COMPETITIVE ADVANTAGES:
                       - Evaluate proposed product's unique differentiators
                       - Assess sustainability of competitive advantages
                       - Identify potential areas of competitive vulnerability
                    
                    4. MARKET POSITIONING:
                       - Analyze market positioning opportunities and challenges
                       - Identify white space and underserved segments
                       - Assess brand positioning requirements
                    
                    5. COMPETITIVE THREATS:
                       - Identify key competitive risks and response strategies
                       - Assess barriers to entry and competitive moats
                       - Evaluate potential for price wars or feature battles
                    
                    6. RECOMMENDATIONS:
                       - Suggest competitive positioning strategies
                       - Recommend feature prioritization based on competition
                       - Advise on go-to-market timing considerations
                    
                    Format your response as: COMPETITIVE_ANALYSIS: [your detailed analysis]
                    """, productIdea);

                String analysis = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "COMPETITIVE_ANALYSIS: " + analysis;
            } catch (Exception e) {
                log.error("Error performing competitive analysis with AI", e);
                return "COMPETITIVE_ANALYSIS_ERROR: Failed to analyze competition - " + e.getMessage();
            }
        });
    }
}
