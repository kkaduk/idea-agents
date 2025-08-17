package io.a2a.poc.agents.idea.confg;


import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class SpringAIConfiguration {

    @Bean
    @Primary
    public ChatClient primaryChatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("You are an expert banking and financial services consultant with deep knowledge of regulatory compliance, risk management, and product development.")
                .build();
    }
}