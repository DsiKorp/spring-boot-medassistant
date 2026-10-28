package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.ChatClientTestSupport;
import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.config.MedicalAuditAdvisor;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryTypeEnum;
import com.dsikorp.iamedassistan.service.AnalysisService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoutingWorkflowServiceimplTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private AnalysisService analysisService;

    @Mock
    private ChatModel chatModel;

    private RoutingWorkflowServiceimpl service;

    @BeforeEach
    void setUp() {
        service = new RoutingWorkflowServiceimpl(clientResolver, analysisService, new MedicalAuditAdvisor());

        ReflectionTestUtils.setField(service, "emergencyResource", new ClassPathResource("prompts/routing-emergency.st"));
        ReflectionTestUtils.setField(service, "symptomResource", new ClassPathResource("prompts/routing-symptom.st"));
        ReflectionTestUtils.setField(service, "generalResource", new ClassPathResource("prompts/routing-general.st"));
        ReflectionTestUtils.setField(service, "prescriptionResource", new ClassPathResource("prompts/routing-prescription.st"));

        service.init();
    }

    private void classifyAs(QueryTypeEnum type) {
        when(analysisService.classifyQuery(anyString(), any(), any()))
                .thenReturn(new QueryClassificationDto(type, "motivo de prueba"));
    }

    @Test
    @DisplayName("una consulta fuera de tema se responde sin llamar al modelo")
    void offTopicIsAnsweredWithoutCallingTheModel() {
        classifyAs(QueryTypeEnum.OFF_TOPIC);

        String response = service.routeQuery("¿cuál es el clima?", "gemini", 7L);

        assertThat(response).contains("solo puedo ayudarte con consultas relacionadas con salud");
        verifyNoInteractions(clientResolver);
    }

    @Test
    @DisplayName("una urgencia usa la plantilla de emergencias y el system prompt de urgencias")
    void emergencyRouteUsesEmergencyTemplate() {
        classifyAs(QueryTypeEnum.EMERGENCY);

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Llamá al 107");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            String response = service.routeQuery("me duele el pecho", "gemini", 7L);

            assertThat(response).isEqualTo("Llamá al 107");

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue()).contains("URGENCIA MÉDICA").contains("me duele el pecho");

            ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).system(systemPrompt.capture());
            assertThat(systemPrompt.getValue()).contains("urgencias");
        }
    }

    @Test
    @DisplayName("un reporte de síntomas usa la plantilla de análisis de síntomas")
    void symptomRouteUsesSymptomTemplate() {
        classifyAs(QueryTypeEnum.SYMPTOM_REPORT);

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Análisis de síntomas");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.routeQuery("tengo fiebre y tos", "gemini", 7L);

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("Un paciente reporta síntomas")
                    .contains("tengo fiebre y tos");
        }
    }

    @Test
    @DisplayName("una pregunta general usa la plantilla de consulta educativa")
    void generalRouteUsesGeneralTemplate() {
        classifyAs(QueryTypeEnum.GENERAL_QUESTION);

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Explicación educativa");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.routeQuery("¿qué es la hipertensión?", "gemini", 7L);

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("consulta médica educativa")
                    .contains("¿qué es la hipertensión?");
        }
    }

    @Test
    @DisplayName("un pedido de medicación usa la plantilla de farmacología")
    void prescriptionRouteUsesPrescriptionTemplate() {
        classifyAs(QueryTypeEnum.PRESCRIPTION_REQUEST);

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Información del medicamento");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.routeQuery("recetame ibuprofeno", "gemini", 7L);

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("medicación o recetas")
                    .contains("recetame ibuprofeno");

            ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).system(systemPrompt.capture());
            assertThat(systemPrompt.getValue()).contains("farmacólogo");
        }
    }

    @Test
    @DisplayName("cada ruta renderiza su propia plantilla, no una genérica")
    void eachRouteRendersItsOwnTemplate() {
        classifyAs(QueryTypeEnum.EMERGENCY);

        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.routeQuery("me desmayé", "gemini", 7L);

            ArgumentCaptor<String> userPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).user(userPrompt.capture());
            assertThat(userPrompt.getValue())
                    .contains("servicios de emergencia")
                    .doesNotContain("Un paciente reporta síntomas");
        }
    }

    @Test
    @DisplayName("la clasificación se pide con el query, el modelo y el userId del paciente")
    void classificationReceivesQueryModelAndUserId() {
        classifyAs(QueryTypeEnum.OFF_TOPIC);

        service.routeQuery("¿cuál es el clima?", "groq", 42L);

        verify(analysisService).classifyQuery("¿cuál es el clima?", "groq", 42L);
    }
}
