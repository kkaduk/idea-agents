package io.a2a.poc.agents.idea.confg;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.annotation.Retryable;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import reactor.netty.http.client.HttpClient;

@Configuration
@EnableRetry
public class AgentConfig {

        @Value("${app.openai.timeout.read:360000}")
        private int readTimeoutSeconds;

        @Value("${app.openai.timeout.connect:30000}")
        private int connectTimeoutSeconds;

        @Value("${aapp.openai..keepalive:true}")
        private boolean keepalive;

        @Bean
        @Primary
        public WebClientCustomizer openAiWebClientCustomizer() {
                return webClientBuilder -> {
                        HttpClient httpClient = HttpClient.create()
                                        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                                        .option(ChannelOption.SO_KEEPALIVE, true)
                                        .doOnConnected(
                                                        conn -> conn.addHandlerLast(new ReadTimeoutHandler(
                                                                        readTimeoutSeconds, TimeUnit.SECONDS)))
                                        .responseTimeout(Duration.ofSeconds(readTimeoutSeconds));

                        webClientBuilder.clientConnector(
                                        new org.springframework.http.client.reactive.ReactorClientHttpConnector(
                                                        httpClient));
                };
        }

        @Bean("ideaGenerationChatClient")
        @Retryable(value = {
                        io.netty.handler.timeout.ReadTimeoutException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
        public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
                MessageWindowChatMemory mem = MessageWindowChatMemory.builder()
                                .maxMessages(10)
                                .build();
                return ChatClient.builder(openAiChatModel)
                                .defaultAdvisors(new SimpleLoggerAdvisor())
                                // .defaultToolCallbacks(tools)
                                .defaultAdvisors(MessageChatMemoryAdvisor.builder(mem)
                                                .build())
                                .defaultSystem("""
                                                You are a creative banking product innovation specialist with expertise in:
                                                - Regulatory analysis and compliance requirements
                                                - Financial product design and market opportunities
                                                - Digital banking trends and customer needs
                                                - Fintech innovation and emerging technologies

                                                Your role is to analyze new regulations and create innovative banking product ideas that:
                                                1. Leverage regulatory opportunities
                                                2. Address market gaps
                                                3. Provide clear customer value propositions
                                                4. Are technically and commercially feasible
                                                """)
                                .build();
        }
}
