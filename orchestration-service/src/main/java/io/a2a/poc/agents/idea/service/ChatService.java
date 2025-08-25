package io.a2a.poc.agents.idea.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class ChatService {
    private final ChatClient chatClient; //Blocking, for DEMO, target Webfliux!

    public ChatService(ChatClient chatClient) { this.chatClient = chatClient; }

    public Mono<String> ask(String prompt) {
        return Mono.fromCallable(() -> chatClient
                .prompt()
                .user(prompt)
                .call()             // (KK) BLOCKING call, demo only
                .content())
            .subscribeOn(Schedulers.boundedElastic());
    }
}
