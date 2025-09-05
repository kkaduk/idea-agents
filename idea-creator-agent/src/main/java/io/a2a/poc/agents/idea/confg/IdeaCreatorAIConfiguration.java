// package io.a2a.poc.agents.idea.confg;

// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class IdeaCreatorAIConfiguration {

//     @Bean("ideaGenerationChatClient")
//     public ChatClient ideaGenerationChatClient(ChatModel chatModel) {
//         return ChatClient.builder(chatModel)
//                 .defaultSystem("""
//                     You are a creative banking product innovation specialist with expertise in:
//                     - Regulatory analysis and compliance requirements
//                     - Financial product design and market opportunities
//                     - Digital banking trends and customer needs
//                     - Fintech innovation and emerging technologies
                    
//                     Your role is to analyze new regulations and create innovative banking product ideas that:
//                     1. Leverage regulatory opportunities
//                     2. Address market gaps
//                     3. Provide clear customer value propositions
//                     4. Are technically and commercially feasible
//                     """)
//                 .build();
//     }
// }