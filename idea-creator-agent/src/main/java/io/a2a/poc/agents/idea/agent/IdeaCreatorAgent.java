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
    name = "IdeaCreatorAgent",
    version = "1.0.0",
    description = "Uses AI to monitor legislation and create banking product propositions",
    url = "http://localhost:8081"
)
@RequiredArgsConstructor
@Slf4j
public class IdeaCreatorAgent {

    @Qualifier("ideaGenerationChatClient")
    private final ChatClient chatClient;

    @A2AAgentSkill(
        id = "analyze-legislation",
        name = "Analyze New Legislation with AI",
        description = "Uses AI to analyze government legislation and identify banking opportunities",
        tags = {"legislation", "ai-analysis", "compliance", "opportunity-detection"},
        examples = {"Analyze new PCI DSS compliance requirements", "Review GDPR updates for banking"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> analyzeLegislation(String legislationText) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Analyzing legislation with AI: {}", legislationText.substring(0, Math.min(100, legislationText.length())));
                
                String prompt = String.format("""
                    Analyze the following new legislation/regulation for banking product opportunities:
                    
                    LEGISLATION TEXT:
                    %s
                    
                    Please provide:
                    1. Key regulatory requirements and changes
                    2. Compliance deadlines and implementation timelines  
                    3. Specific opportunities for new banking products or services
                    4. Market impact assessment and competitive implications
                    5. Technical or operational requirements that banks must address
                    
                    Format your response as: LEGISLATION_ANALYSIS: [your detailed analysis]
                    """, legislationText);

                String analysis = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "LEGISLATION_ANALYSIS: " + analysis;
                
            } catch (Exception e) {
                log.error("Error analyzing legislation with AI", e);
                return "LEGISLATION_ANALYSIS_ERROR: Failed to analyze legislation - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "create-product-idea",
        name = "Create Banking Product Idea with AI",
        description = "Uses AI to formulate detailed banking product propositions based on opportunities",
        tags = {"product-development", "ai-innovation", "banking", "proposition"},
        examples = {"Create eco-friendly loan product based on green regulations", "Develop digital identity verification service"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> createProductIdea(String analysisOrOpportunity) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Creating product idea with AI based on: {}", analysisOrOpportunity.substring(0, Math.min(100, analysisOrOpportunity.length())));
                
                String prompt = String.format("""
                    Based on the following regulatory analysis or market opportunity, create a comprehensive banking product proposition:
                    
                    ANALYSIS/OPPORTUNITY:
                    %s
                    
                    Please develop a detailed banking product idea that includes:
                    1. Product Name and Category (e.g., Digital Banking, Lending, Payments, etc.)
                    2. Target Customer Segments (retail, SME, corporate)
                    3. Core Features and Functionality
                    4. Value Proposition and Competitive Advantages
                    5. Revenue Model and Pricing Strategy
                    6. Technology Requirements and Integration Points
                    7. Regulatory Compliance Considerations
                    8. Implementation Timeline (high-level phases)
                    9. Success Metrics and KPIs
                    10. Go-to-Market Strategy Overview
                    
                    Ensure the product idea is innovative, technically feasible, and addresses real market needs.
                    Format your response as: PRODUCT_IDEA: [your detailed product proposition]
                    """, analysisOrOpportunity);

                String productIdea = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "PRODUCT_IDEA: " + productIdea;
                
            } catch (Exception e) {
                log.error("Error creating product idea with AI", e);
                return "PRODUCT_IDEA_ERROR: Failed to create product idea - " + e.getMessage();
            }
        });
    }
}