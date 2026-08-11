package com.dsikorp.iamedassistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class ClientResolver {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
    }

    public ChatClient resolve(String model){
        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
    }
}
