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
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClinicalWorkerTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private ChatModel chatModel;

    private ClinicalWorker worker;

    @BeforeEach
    void setUp() {
        worker = new ClinicalWorker(clientResolver);

        ReflectionTestUtils.setField(worker, "clinicalResource", new ClassPathResource("prompts/worker-clinical.st"));

        worker.init();
    }

    @Test
    @DisplayName("el worker clínico devuelve el análisis del modelo")
    void returnsModelAnalysis() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Análisis clínico del paciente");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            String result = worker.analyze("fiebre y tos", "gemini");

            assertThat(result).isEqualTo("Análisis clínico del paciente");
        }
    }

    @Test
    @DisplayName("el prompt del worker renderiza la plantilla clínica con los síntomas")
    void rendersClinicalTemplateWithSymptoms() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            worker.analyze("fiebre, tos seca y dolor muscular", "gemini");

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());

            assertThat(userPrompt.getValue())
                    .contains("especialista en razonamiento clínico")
                    .contains("fiebre, tos seca y dolor muscular");

            ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).system(systemPrompt.capture());
            assertThat(systemPrompt.getValue()).contains("razonamiento clínico");
        }
    }
}
