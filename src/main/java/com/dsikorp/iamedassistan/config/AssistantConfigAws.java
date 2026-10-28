package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.tools.*;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Configuration
@Profile("aws")
@RequiredArgsConstructor
public class AssistantConfigAws {

    @Value("classpath:prompts/system-prompt.st")
    private Resource systemPromptResource;

    private final AppointmentSearchTool appointmentSearchTool;
    private final DoctorInfoTool doctorInfoTool;
    private final PatientInfoTool patientInfoTool;
    private final DrugInfoTool drugInfoTool;
    private final AppointmentBookingTool appointmentBookingTool;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    @Bean("geminiClient")
    ChatClient geminiClient(
            GoogleGenAiChatModel chatModel,
            @Qualifier("googleVectorStore") VectorStore vectorStore) throws IOException {

        String systemPrompt = systemPromptResource.getContentAsString(StandardCharsets.UTF_8)
                .replace("{currentDate}", LocalDate.now().toString());

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultTools(appointmentSearchTool, doctorInfoTool,
                        patientInfoTool, drugInfoTool, appointmentBookingTool)
                .defaultAdvisors(QuestionAnswerAdvisor.builder(vectorStore)
                        .searchRequest(SearchRequest.builder()
                                .similarityThreshold(0.7).topK(3).build())
                        .promptTemplate(new PromptTemplate("""
                Contexto de documentos médicos (usalo si es relevante para la pregunta):
                {question_answer_context}
                
                Si la información no está en los documentos, podés usar tus herramientas
                o tu conocimiento general para responder.
                """))
                        .build(), medicalAuditAdvisor)
                .build();
    }

    @Bean("googleVectorStore")
    VectorStore googleVectorStore(
            @Qualifier("googleGenAiTextEmbedding") EmbeddingModel embeddingModel,
            JdbcTemplate jdbcTemplate){
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(3072)
                .indexType(PgVectorStore.PgIndexType.NONE)
                .initializeSchema(true)
                .vectorTableName("medassistant_google")
                .build();
    }
}


















