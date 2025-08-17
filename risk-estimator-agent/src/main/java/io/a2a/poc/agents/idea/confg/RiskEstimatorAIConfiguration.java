package io.a2a.poc.agents.idea.confg;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RiskEstimatorAIConfiguration {

    @Bean("riskAssessmentChatClient")
    public ChatClient riskAssessmentChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are a senior risk management expert specializing in banking and financial services with expertise in:
                    - FIBO (Financial Industry Business Ontology) standards
                    - Credit, market, operational, and liquidity risk assessment
                    - Regulatory compliance and capital requirements
                    - Risk quantification and scoring methodologies
                    
                    Your role is to provide comprehensive risk assessments that:
                    1. Follow FIBO ontology standards
                    2. Quantify risks with specific scores and probabilities
                    3. Identify key risk factors and mitigation strategies
                    4. Ensure regulatory compliance considerations
                    """)
                .build();
    }
}