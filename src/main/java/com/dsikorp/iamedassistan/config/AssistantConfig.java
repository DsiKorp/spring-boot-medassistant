package com.dsikorp.iamedassistan.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

//    @Bean
//    ChatClient chatClient(ChatClient.Builder builder) {
//        return builder.build();
//    }

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel)  throws IOException{
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel)  throws IOException{
        return ChatClient.builder(chatModel)
                .defaultSystem(systemPromptResource.getContentAsString(StandardCharsets.UTF_8))
                .build();
    }
}
