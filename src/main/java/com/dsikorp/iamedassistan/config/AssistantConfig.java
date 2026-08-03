package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tool.AppointmentSearchTool;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;

//    @Bean
//    ChatClient chatClient(ChatClient.Builder builder) {
//        return builder.build();
//    }

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel)  throws IOException{

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool)
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel)  throws IOException{
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool)
                .build();
    }
}
