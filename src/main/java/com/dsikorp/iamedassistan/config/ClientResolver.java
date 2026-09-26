package com.dsikorp.iamedassistan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientResolver {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;
    private final ChatClient openaiClient;
    private final ChatClient anthropicClient;
    private final ChatClient groqClient;
    private final ChatClient minimaxClient;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("openiaClient") ChatClient openaiClient,
            @Qualifier("anthropicClient") ChatClient anthropicClient,
            @Qualifier("groqClient") ChatClient groqClient,
            @Qualifier("minimaxClient") ChatClient minimaxClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
        this.openaiClient = openaiClient;
        this.anthropicClient = anthropicClient;
        this.groqClient = groqClient;
        this.minimaxClient = minimaxClient;
    }

    public ChatClient resolve(String model){
        log.info("ClientResolver modelo: {}", model);

        if ("ollama".equalsIgnoreCase(model)) {
            return ollamaClient;
        }
        if ("openai".equalsIgnoreCase(model)) {
            return openaiClient;
        }
        if ("anthropic".equalsIgnoreCase(model)) {
            return anthropicClient;
        }
        if ("groq".equalsIgnoreCase(model)) {
            return groqClient;
        }
        if ("minimax".equalsIgnoreCase(model)) {
            return minimaxClient;
        }

        return geminiClient;
    }
}