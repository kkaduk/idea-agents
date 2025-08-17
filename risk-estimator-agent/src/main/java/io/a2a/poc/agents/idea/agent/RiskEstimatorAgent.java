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
    name = "RiskEstimatorAgent",
    version = "1.0.0",
    description = "Uses AI to assess financial and operational risks using FIBO standards",
    url = "http://localhost:8082"
)
@RequiredArgsConstructor
@Slf4j
public class RiskEstimatorAgent {

    @Qualifier("riskAssessmentChatClient")
    private final ChatClient chatClient;

    @A2AAgentSkill(
        id = "assess-comprehensive-risk",
        name = "Comprehensive Risk Assessment with AI",
        description = "Uses AI to analyze all risk categories using FIBO ontology standards",
        tags = {"risk-assessment", "ai-analysis", "FIBO", "comprehensive"},
        examples = {"Assess all risks for new digital payment product", "Evaluate comprehensive risk profile for lending service"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> assessComprehensiveRisk(String productDescription) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Performing comprehensive risk assessment with AI for: {}", 
                        productDescription.substring(0, Math.min(100, productDescription.length())));
                
                String prompt = String.format("""
                    Perform a comprehensive risk assessment for the following banking product using FIBO (Financial Industry Business Ontology) standards:
                    
                    PRODUCT DESCRIPTION:
                    %s
                    
                    Please provide a detailed risk assessment covering:
                    
                    1. CREDIT RISK ASSESSMENT:
                       - Default probability and expected losses
                       - Credit concentration risks
                       - Collateral and guarantee adequacy
                       - Risk score (0-100) with justification
                    
                    2. MARKET RISK ASSESSMENT:
                       - Interest rate risk exposure
                       - Currency and commodity risks
                       - Market volatility impact
                       - Risk score (0-100) with justification
                    
                    3. OPERATIONAL RISK ASSESSMENT:
                       - Technology and system risks
                       - Process and human error risks
                       - Fraud and security risks
                       - Compliance and regulatory risks
                       - Risk score (0-100) with justification
                    
                    4. LIQUIDITY RISK ASSESSMENT:
                       - Funding liquidity risks
                       - Market liquidity risks
                       - Cash flow and timing mismatches
                       - Risk score (0-100) with justification
                    
                    5. OVERALL RISK PROFILE:
                       - Aggregate risk score (0-100)
                       - Risk level classification (LOW/MEDIUM/HIGH/CRITICAL)
                       - Key risk drivers and dependencies
                       - Recommended risk mitigation strategies
                       - Capital adequacy requirements
                       - Regulatory compliance implications
                    
                    Use quantitative analysis where possible and provide specific risk metrics.
                    Format your response as: COMPREHENSIVE_RISK_ASSESSMENT: [your detailed assessment]
                    """, productDescription);

                String riskAssessment = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "COMPREHENSIVE_RISK_ASSESSMENT: " + riskAssessment;
                
            } catch (Exception e) {
                log.error("Error performing comprehensive risk assessment with AI", e);
                return "RISK_ASSESSMENT_ERROR: Failed to assess risks - " + e.getMessage();
            }
        });
    }

    @A2AAgentSkill(
        id = "assess-regulatory-compliance",
        name = "Regulatory Compliance Risk Assessment with AI",
        description = "Uses AI to evaluate regulatory compliance requirements and associated risks",
        tags = {"compliance", "ai-analysis", "regulatory", "FIBO"},
        examples = {"Assess Basel III compliance requirements", "Evaluate GDPR compliance risks"},
        inputModes = {"text"},
        outputModes = {"text"}
    )
    public CompletableFuture<String> assessRegulatoryCompliance(String productDescription) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Assessing regulatory compliance with AI for: {}", 
                        productDescription.substring(0, Math.min(100, productDescription.length())));
                
                String prompt = String.format("""
                    Assess the regulatory compliance requirements and risks for the following banking product:
                    
                    PRODUCT DESCRIPTION:
                    %s
                    
                    Please analyze:
                    
                    1. APPLICABLE REGULATIONS:
                       - Primary regulatory frameworks (Basel III, MiFID II, PSD2, GDPR, etc.)
                       - Jurisdictional requirements and variations
                       - Industry-specific compliance standards
                    
                    2. COMPLIANCE REQUIREMENTS:
                       - Capital adequacy and reserve requirements
                       - Reporting and disclosure obligations
                       - Consumer protection requirements
                       - Data privacy and security standards
                       - Anti-money laundering (AML) requirements
                    
                    3. COMPLIANCE RISKS:
                       - Regulatory breach probability and impact
                       - Penalty and sanction exposure
                       - Reputational risk implications
                       - License and authorization risks
                    
                    4. COMPLIANCE SCORE:
                       - Overall compliance risk score (0-100, where 0 is fully compliant, 100 is high risk)
                       - Compliance readiness assessment
                       - Implementation timeline requirements
                    
                    5. MITIGATION RECOMMENDATIONS:
                       - Required compliance controls and procedures
                       - Technology and system requirements
                       - Staff training and governance needs
                       - Ongoing monitoring and reporting processes
                    
                    Format your response as: REGULATORY_COMPLIANCE_ASSESSMENT: [your detailed assessment]
                    """, productDescription);

                String complianceAssessment = chatClient.prompt()
                    .user(prompt)
                    .call()
                    .content();

                return "REGULATORY_COMPLIANCE_ASSESSMENT: " + complianceAssessment;
                
            } catch (Exception e) {
                log.error("Error assessing regulatory compliance with AI", e);
                return "COMPLIANCE_ASSESSMENT_ERROR: Failed to assess compliance - " + e.getMessage();
            }
        });
    }
}