package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tool.AppointmentSearchTool;
import com.dsikorp.iamedassistan.tool.DoctorInfoTool;
import com.anthropic.models.messages.Model;
import com.dsikorp.iamedassistan.tool.PatientInfoTool;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
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
    private final PatientInfoTool patientInfoTool;

    /**
     * Obtiene el prompt del sistema desde un archivo, reemplazando el placeholder
     * {currentDate} con la fecha actual. Debe ser llamado antes de crear cualquier
     * cliente de chat para establecer su system prompt.
     */
    private String getSystemPrompt() throws IOException {
        return systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());
    }

//    @Bean  // si fuera un solo modelo
//    ChatClient chatClient(ChatClient.Builder builder) {
//        return builder.build();
//    }

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
                .build();
    }

    @Bean("openiaClient")
    ChatClient openiaClient(OpenAiChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
                .build();
    }

    @Bean("anthropicClient")
    ChatClient anthropicClient(AnthropicChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
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

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
                .build();
    }

    /**
     * MiniMax expone una API Anthropic-compatible en {@code https://api.minimax.io/anthropic},
     * así que reutilizamos {@link AnthropicChatModel} con una {@link AnthropicChatOptions}
     * apuntada a su base-url. {@code AnthropicChatModel.builder()} construye internamente el
     * cliente de Anthropic a partir de {@code baseUrl} y {@code apiKey} de las options.
     * Se mantiene como bean independiente para no colisionar con la auto-config nativa
     * de Anthropic (que sigue creando su propio {@code AnthropicChatModel} a partir de
     * {@code spring.ai.anthropic.*}).
     * <p>
     * Se usa {@link Model#of(String)} porque {@code MiniMax-M3[1m]} no es un modelo
     * conocido del enum de Anthropic.
     */
    @Bean("minimaxClient")
    ChatClient minimaxClient(
            @Value("${spring.ai.minimax.api-key}") String apiKey,
            @Value("${spring.ai.minimax.base-url}") String baseUrl,
            @Value("${spring.ai.minimax.chat.options.model}") String model
    ) throws IOException {
        AnthropicChatOptions options = AnthropicChatOptions.builder()
                .model(Model.of(model))
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .temperature(0.2)
                .topP(0.85)
                .maxTokens(2048)
                .build();

        AnthropicChatModel chatModel = AnthropicChatModel.builder()
                .options(options)
                .observationRegistry(ObservationRegistry.NOOP)
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(appointmentSearchTool, doctorInfoTool, patientInfoTool)
                .build();
    }
}
