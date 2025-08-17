package io.a2a.poc.agents.idea.util;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import io.a2a.poc.agents.idea.service.IdeaProductWorkflowOrchestrator.A2AReceptionistSkill;

@Service
public class PlannerService {

    private final ChatClient chatClient;

    public PlannerService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String plan(PlannerPromptBuilder.UserTask task,
                       List<A2AReceptionistSkill> catalog,
                       double minConfidence) {

        String prompt = PlannerPromptBuilder.build(task, catalog, minConfidence);

        // Synchronous (DEMO) call to the chat client
        // Note: This will block until the response is received.
        return chatClient
                .prompt(prompt)
                .call()
                .content();

        // return chatClient.prompt(prompt).stream().collect(Collectors.joining());
    }
}