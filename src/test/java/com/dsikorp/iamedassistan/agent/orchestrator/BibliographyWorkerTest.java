package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.ChatClientTestSupport;
import com.dsikorp.iamedassistan.config.ClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BibliographyWorkerTest {

    private static final String SINTOMAS = "fiebre y tos";

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatModel chatModel;

    private BibliographyWorker worker;

    @BeforeEach
    void setUp() {
        worker = new BibliographyWorker(clientResolver);

        ReflectionTestUtils.setField(worker, "bibliographyResource", new ClassPathResource("prompts/worker-bibliography.st"));

        worker.init();
    }

    @Test
    @DisplayName("informa que no hay documentos relevantes cuando la búsqueda no devuelve nada")
    void reportsNoRelevantDocuments() {
        when(clientResolver.resolveVectorStore("gemini")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        String result = worker.research(SINTOMAS, "gemini");

        assertThat(result).isEqualTo("No se encontraron documentos médicos relevantes para estos síntomas.");
    }

    @Test
    @DisplayName("la búsqueda semántica usa topK 3 y umbral de similitud 0.3")
    void searchesWithExpectedTuning() {
        when(clientResolver.resolveVectorStore("gemini")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        worker.research(SINTOMAS, "gemini");

        ArgumentCaptor<SearchRequest> searchRequest = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(searchRequest.capture());

        assertThat(searchRequest.getValue().getQuery()).isEqualTo(SINTOMAS);
        assertThat(searchRequest.getValue().getTopK()).isEqualTo(3);
        assertThat(searchRequest.getValue().getSimilarityThreshold()).isEqualTo(0.3);
    }

    @Test
    @DisplayName("interpreta los documentos encontrados y devuelve el resumen")
    void interpretsFoundDocuments() {
        when(clientResolver.resolveVectorStore("gemini")).thenReturn(vectorStore);
        when(vectorStore.similaritySearch(any(SearchRequest.class)))
                .thenReturn(List.of(new Document("La fiebre puede indicar infección.")));

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Resumen de la bibliografía");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            String result = worker.research(SINTOMAS, "gemini");

            assertThat(result).isEqualTo("Resumen de la bibliografía");

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("La fiebre puede indicar infección.")
                    .contains(SINTOMAS);
        }
    }
}
