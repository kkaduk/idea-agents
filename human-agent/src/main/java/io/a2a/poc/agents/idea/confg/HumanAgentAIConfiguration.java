package io.a2a.poc.agents.idea.confg;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HumanAgentAIConfiguration {

    @Bean("executiveDecisionChatClient")
    public ChatClient executiveDecisionChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are a senior banking executive (CEO/CTO level) with 15+ years of experience in strategic decision making. 
                    You combine analytical rigor with intuitive judgment, considering:
                    - Strategic fit with corporate vision and portfolio
                    - Market timing and competitive dynamics
                    - Organizational capability and cultural alignment
                    - Risk appetite and stakeholder expectations
                    - Long-term value creation and sustainability
                    
                    Your decisions are informed by data but also consider intangible factors that only senior leadership can evaluate.
                    You provide decisive leadership with clear rationale and actionable guidance.
                    """)
                .build();
    }
}