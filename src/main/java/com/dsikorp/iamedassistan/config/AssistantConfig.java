package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tool.AppointmentSearchTool;
import com.dsikorp.iamedassistan.tool.DoctorInfoTool;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientAsync;

@Configuration
@RequiredArgsConstructor
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;

//    @Bean
//    ChatClient chatClient(ChatClient.Builder builder) {
//        return builder.build();
//    }

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }

    @Bean("openiaClient")
    ChatClient openiaClient(OpenAiChatModel chatModel) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }

    @Bean("anthropicClient")
    ChatClient anthropicClient(AnthropicChatModel chatModel) throws IOException {
        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }

    /**
     * Groq expone una API OpenAI-compatible, así que reutilizamos {@link OpenAiChatModel}
     * construido manualmente con {@link OpenAiSetup} apuntando a la base-url de Groq.
     * Se mantiene como bean independiente para no colisionar con la auto-config de OpenAI
     * (que sigue creando su propio {@link OpenAiChatModel} a partir de spring.ai.openai.*).
     */
    @Bean("groqClient")
    ChatClient groqClient(
            @Value("${spring.ai.groq.api-key}") String apiKey,
            @Value("${spring.ai.groq.base-url}") String baseUrl,
            @Value("${spring.ai.groq.chat.options.model}") String model
    ) throws IOException {
        OpenAIClient syncClient = OpenAiSetup.setupSyncClient(
                baseUrl, apiKey,
                null, null, null, null,
                false, false,
                model,
                Duration.ofSeconds(60),
                3,
                null, null,
                ObservationRegistry.NOOP,
                null,
                List.of()
        );
        OpenAIClientAsync asyncClient = OpenAiSetup.setupAsyncClient(
                baseUrl, apiKey,
                null, null, null, null,
                false, false,
                model,
                Duration.ofSeconds(60),
                3,
                null, null,
                ObservationRegistry.NOOP,
                null,
                List.of()
        );

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.2)
                .topP(0.85)
                .maxTokens(2048)
                .build();

        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiClient(syncClient)
                .openAiClientAsync(asyncClient)
                .options(options)
                .build();

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool)
                .build();
    }
}
