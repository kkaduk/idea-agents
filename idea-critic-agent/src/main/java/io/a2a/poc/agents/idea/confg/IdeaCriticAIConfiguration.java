package io.a2a.poc.agents.idea.confg;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdeaCriticAIConfiguration {

    @Bean("criticAnalysisChatClient")
    public ChatClient criticAnalysisChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    You are a critical business analyst with expertise in:
                    - Market analysis and competitive intelligence
                    - Customer experience and user interface design
                    - Technical feasibility and implementation challenges
                    - Business model validation and revenue sustainability
                    
                    Your role is to provide constructive criticism that:
                    1. Identifies potential weaknesses and blind spots
                    2. Challenges assumptions with data-driven insights
                    3. Suggests specific improvements and alternatives
                    4. Evaluates market viability and competitive positioning
                    """)
                .build();
    }
}