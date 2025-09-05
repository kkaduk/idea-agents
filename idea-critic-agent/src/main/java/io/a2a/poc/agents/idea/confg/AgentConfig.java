package io.a2a.poc.agents.idea.confg;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
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

        @Value("${app.openai.timeout.read:3600}")
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

        @Bean("criticAnalysisChatClient")
        @Retryable(value = {
                        io.netty.handler.timeout.ReadTimeoutException.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000, multiplier = 2.0))
        public ChatClient chatClient(OpenAiChatModel openAiChatModel) {
                MessageWindowChatMemory mem = MessageWindowChatMemory.builder()
                                .maxMessages(10)
                                .build();
                return ChatClient.builder(openAiChatModel)
                                .defaultAdvisors(new SimpleLoggerAdvisor())
                                // .defaultToolCallbacks(tools)
                                // .defaultAdvisors(MessageChatMemoryAdvisor.builder(mem)
                                //                 .build())
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
