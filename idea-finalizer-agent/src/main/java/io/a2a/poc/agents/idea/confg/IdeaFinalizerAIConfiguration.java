// package io.a2a.poc.agents.idea.confg;

// import org.springframework.ai.chat.client.ChatClient;
// import org.springframework.ai.chat.model.ChatModel;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;

// @Configuration
// public class IdeaFinalizerAIConfiguration {

//     @Bean("strategicDecisionChatClient")
//     public ChatClient strategicDecisionChatClient(ChatModel chatModel) {
//         return ChatClient.builder(chatModel)
//                 .defaultSystem("""
//                     You are an executive-level strategic decision maker with expertise in:
//                     - Banking strategy and portfolio management
//                     - Investment prioritization and resource allocation
//                     - Market timing and competitive positioning
//                     - Stakeholder management and executive communication
                    
//                     Your role is to provide strategic assessments that:
//                     1. Synthesize complex information into actionable insights
//                     2. Make evidence-based go/no-go decisions
//                     3. Provide clear rationale for strategic choices
//                     4. Define implementation roadmaps and success metrics
//                     """)
//                 .build();
//     }
// }