package com.dsikorp.iamedassistan.agent.OpenAgent;

import com.dsikorp.iamedassistan.ChatClientTestSupport;
import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.tools.AppointmentSearchTool;
import com.dsikorp.iamedassistan.tools.DoctorInfoTool;
import com.dsikorp.iamedassistan.tools.DrugInfoTool;
import com.dsikorp.iamedassistan.tools.PatientInfoTool;
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

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAgentServiceImplTest {

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private PatientInfoTool patientInfoTool;

    @Mock
    private AppointmentSearchTool appointmentSearchTool;

    @Mock
    private DoctorInfoTool doctorInfoTool;

    @Mock
    private DrugInfoTool drugInfoTool;

    @Mock
    private ChatModel chatModel;

    private OpenAgentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OpenAgentServiceImpl(clientResolver, patientInfoTool, appointmentSearchTool, doctorInfoTool, drugInfoTool);

        ReflectionTestUtils.setField(service, "agentResource", new ClassPathResource("prompts/open-agent.st"));

        service.init();
    }

    @Test
    @DisplayName("el agente devuelve la respuesta del modelo")
    void returnsModelResponse() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("Respuesta del agente abierto");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            String response = service.execute("¿qué turnos hay?", "gemini", 7L);

            assertThat(response).isEqualTo("Respuesta del agente abierto");
        }
    }

    @Test
    @DisplayName("el agente recibe las cuatro tools de consulta")
    void passesConsultationToolsToTheModel() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.execute("consulta", "gemini", 7L);

            verify(mock.request()).tools(patientInfoTool, appointmentSearchTool, doctorInfoTool, drugInfoTool);
        }
    }

    @Test
    @DisplayName("el agente recibe el userId y el rol en el contexto de tools")
    void passesUserContextToTheTools() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.execute("consulta", "gemini", 42L);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Object>> toolContext = ArgumentCaptor.forClass(Map.class);
            verify(mock.request()).toolContext(toolContext.capture());

            assertThat(toolContext.getValue())
                    .containsEntry("userId", 42L)
                    .containsEntry("role", "PATIENT");
        }
    }

    @Test
    @DisplayName("el system prompt del agente incluye la fecha actual")
    void systemPromptContainsCurrentDate() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.execute("consulta", "gemini", 7L);

            ArgumentCaptor<String> systemPrompt = ArgumentCaptor.forClass(String.class);
            verify(mock.request()).system(systemPrompt.capture());

            assertThat(systemPrompt.getValue())
                    .contains("Sos un agente médico autónomo")
                    .contains("Fecha actual: " + LocalDate.now());
        }
    }

    @Test
    @DisplayName("la consulta del paciente se envía como mensaje de usuario")
    void sendsPatientQueryAsUserMessage() {
        try (MockedStatic<ChatClient> mockedStatic = mockStatic(ChatClient.class)) {
            ChatClientTestSupport.Mock mock = ChatClientTestSupport.mockChatClient();
            when(mock.call().content()).thenReturn("ok");
            when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
            mockedStatic.when(() -> ChatClient.create(any(ChatModel.class))).thenReturn(mock.client());

            service.execute("¿necesito turno con cardiología?", "gemini", 7L);

            verify(mock.request()).user("¿necesito turno con cardiología?");
        }
    }
}
