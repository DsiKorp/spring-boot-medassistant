package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tool.*;
import com.anthropic.models.messages.Model;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
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
@Slf4j
public class AssistantConfig {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final ChatMemory chatMemory;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    /**
     * System prompt cargado una sola vez al arrancar el contexto de Spring
     * (con el placeholder {currentDate} ya sustituido por la fecha actual).
     * Mantenerlo en caché evita releer el classpath en cada request.
     */
    private String systemPrompt;

    /**
     * Carga el system prompt desde el classpath y sustituye {currentDate} por
     * la fecha actual. Si falla, el contexto de Spring no arranca y el error
     * aparece claro en el log de arranque.
     */
    @PostConstruct
    void loadSystemPrompt() throws IOException {
        this.systemPrompt = systemPromptResource
                .getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());
        log.info("System prompt cargado desde classpath ({} caracteres)",
                systemPrompt.length());
    }

    /**
     * Construye el {@link SearchRequest} compartido por todos los beans de
     * {@link ChatClient}: umbral de similitud 0.5 (cosine) y topK=5. Si en el
     * futuro hay que tunear el RAG, se cambia acá una sola vez.
     */
    private SearchRequest buildSearchRequest() {
        return SearchRequest.builder()
                .similarityThreshold(0.5)
                .topK(5)
                .build();
    }

    /**
     * PromptTemplate usado por el {@link QuestionAnswerAdvisor}. A diferencia del
     * template por defecto (que obliga al LLM a responder "no puedo responder"
     * cuando el contexto está vacío), este le indica al asistente que, si los
     * documentos no contienen la respuesta, recurra a sus herramientas
     * ({@code defaultTools}) o a su conocimiento general.
     */
    private PromptTemplate getPromptTemplate() {
        return new PromptTemplate("""
                Contexto de documentos médicos (usalo si es relevante para la pregunta):
                {question_answer_context}
                
                Si la información no está en los documentos, podés usar tus herramientas
                o tu conocimiento general para responder.
                """);
    }

//    @Bean  // si fuera un solo modelo
//    ChatClient chatClient(ChatClient.Builder builder) {
//        return builder.build();
//    }

    @Bean("geminiClient")
    ChatClient geminiClient(GoogleGenAiChatModel chatModel,
                            @Qualifier("googleVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        // config global
                        // genera conversationId cannot be null en endpoints que no manejan esto
                        //MessageChatMemoryAdvisor.builder(chatMemory).build(),

                        // embedding model RAG
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                        // embedding model
                )
                .build();
    }

    @Bean("ollamaClient")
    ChatClient ollamaClient(OllamaChatModel chatModel,
                            @Qualifier("ollamaVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        // MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // embedding model
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                        // embedding model
                )
                .build();
    }

    @Bean("openaiClient")
    ChatClient openaiClient(OpenAiChatModel chatModel,
                            @Qualifier("openaiVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        //MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                )
                .build();
    }

    @Bean("anthropicClient")
    ChatClient anthropicClient(AnthropicChatModel chatModel,
                               @Qualifier("anthropicVectorStore") VectorStore vectorStore) throws IOException {

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        //MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                )
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
            @Value("${spring.ai.groq.chat.options.model}") String model,
            @Qualifier("groqVectorStore") VectorStore vectorStore
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
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        //MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                )
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
            @Value("${spring.ai.minimax.chat.options.model}") String model,
            @Qualifier("minimaxVectorStore") VectorStore vectorStore
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
                .defaultSystem(systemPrompt)
                .defaultTools(
                        appointmentSearchTool,
                        doctorInfoTool,
                        patientInfoTool,
                        drugInfoTool,
                        appointmentBookingTool
                )
                .defaultAdvisors(
                        //MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(buildSearchRequest())
                                .promptTemplate(getPromptTemplate())
                                .build(), medicalAuditAdvisor
                )
                .build();
    }

    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embeddingModel,
            ChromaApi chromaApi) {
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

    @Bean("openaiVectorStore")
    VectorStore openaiVectorStore(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_openai")
                .initializeSchema(true)
                .build();
    }

    @Bean("anthropicVectorStore")
    VectorStore anthropicVectorStore(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_anthropic")
                .initializeSchema(true)
                .build();
    }

    @Bean("groqVectorStore")
    VectorStore groqVectorStore(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_groq")
                .initializeSchema(true)
                .build();
    }

    @Bean("minimaxVectorStore")
    VectorStore minimaxVectorStore(
            @Qualifier("openAiEmbeddingModel") EmbeddingModel embedding,
            ChromaApi chromaApi) {
        return ChromaVectorStore.builder(chromaApi, embedding)
                .collectionName("medassistant_minimax")
                .initializeSchema(true)
                .build();
    }

    @Bean
    ChromaApi chromaApi(@Value("${spring.ai.vectorstore.chroma.url}") String chromaUrl) {
        return ChromaApi.builder()
                .baseUrl(chromaUrl)
                .build();
    }
}
