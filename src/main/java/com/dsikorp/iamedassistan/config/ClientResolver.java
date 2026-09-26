package com.dsikorp.iamedassistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ClientResolver {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;
    private final ChatClient openaiClient;
    private final ChatClient anthropicClient;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("openiaClient") ChatClient openaiClient,
            @Qualifier("anthropicClient") ChatClient anthropicClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
        this.openaiClient = openaiClient;
        this.anthropicClient = anthropicClient;
    }

    public ChatClient resolve(String model){
        if ("ollama".equalsIgnoreCase(model)) {
            return ollamaClient;
        }
        if ("openai".equalsIgnoreCase(model)) {
            return openaiClient;
        }
        if ("anthropic".equalsIgnoreCase(model)) {
            return anthropicClient;
        }
        return geminiClient;
    }
}