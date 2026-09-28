package com.dsikorp.iamedassistan;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.boot.CommandLineRunner;

import java.util.Objects;

//@Component
@RequiredArgsConstructor
public class TestChatClient implements CommandLineRunner {

    private final ChatClient chatClient;

    @Override
    public void run(String... args) throws Exception {
        ChatResponse chatResponse = chatClient
                .prompt("¿Qué es un agujero negro?")
                .call()
                .chatResponse();

        String content = Objects.requireNonNull(chatResponse.getResult()).getOutput().getText();
        System.out.println("=== RESPUESTA ===");
        System.out.println(content);

        System.out.println("\n=== METADATA ===");
        System.out.println("Modelo: " +
                chatResponse.getMetadata().getModel()
                );

        System.out.println("Tokens de entrada: " +
                chatResponse.getMetadata().getUsage().getPromptTokens()
                );
        System.out.println("Tokens de salida: " +
                chatResponse.getMetadata().getUsage().getCompletionTokens()
                );
        System.out.println("Tokens totales: " +
                chatResponse.getMetadata().getUsage().getTotalTokens()
                );

        System.out.println("Finish reason: " +
                chatResponse.getResult().getMetadata().getFinishReason()
                );
    }
}










