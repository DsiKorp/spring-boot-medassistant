package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tool.*;
import com.anthropic.models.messages.Model;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chroma.vectorstore.ChromaApi;
import org.springframework.ai.chroma.vectorstore.ChromaVectorStore;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.setup.OpenAiSetup;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final ChatMemory chatMemory;

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
    ChatClient geminiClient(GoogleGenAiChatModel chatModel,
                            @Qualifier("googleVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // embedding model
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7).topK(3).build()
                                ).build()
                        // embedding model
                )
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel,
                            @Qualifier("ollamaVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // embedding model
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder()
                                        .similarityThreshold(0.7).topK(3).build()
                                ).build()
                        // embedding model
                )
                .build();
    }

    @Bean("openiaClient")
    ChatClient openiaClient(OpenAiChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean("anthropicClient")
    ChatClient anthropicClient(AnthropicChatModel chatModel) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(getSystemPrompt())
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
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
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
            @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel,
            ChromaApi chromaApi){
        return ChromaVectorStore.builder(chromaApi, embeddingModel)
                .collectionName("medassistant_google")
                .initializeSchema(true)
                .build();
    }

    @Bean("ollamaVectorStore")
    VectorStore ollamaVectorStore(
            @Qualifier("ollamaEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_ollama")
                .initializeSchema(true)
                .build();
    }

    @Bean
    ChromaApi chromaApi(@Value("${spring.ai.vectorstore.chroma.url}") String chromaUrl){
        return ChromaApi.builder()
                .baseUrl(chromaUrl)
                .build();
    }
}
