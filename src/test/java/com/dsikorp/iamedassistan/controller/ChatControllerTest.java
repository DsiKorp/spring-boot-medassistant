package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.MockMvcTestSupport;
import com.dsikorp.iamedassistan.service.AssistantService;
import com.dsikorp.iamedassistan.util.SecurityUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AssistantService assistantService;

    @Mock
    private SecurityUtils securityUtils;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.standalone(new ChatController(assistantService, securityUtils)).build();
    }

    @AfterEach
    void tearDown() {
        MockMvcTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("el endpoint de chat devuelve el texto del asistente")
    void chatReturnsAssistantText() throws Exception {
        when(securityUtils.extractRole(any())).thenReturn("PATIENT");
        when(assistantService.chat("¿qué es la hipertensión?", "gemini", USER_ID, "PATIENT"))
                .thenReturn("La hipertensión es...");

        mockMvc.perform(post("/api/v1/chat")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"¿qué es la hipertensión?\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("La hipertensión es..."));
    }

    @Test
    @DisplayName("el userId se toma del claim del JWT y el rol del contexto de seguridad")
    void chatUsesUserIdFromJwtClaimAndRoleFromAuthentication() throws Exception {
        when(securityUtils.extractRole(any())).thenReturn("ADMIN");
        when(assistantService.chat(any(), any(), any(), any())).thenReturn("ok");

        mockMvc.perform(post("/api/v1/chat")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"hola\"}"))
                .andExpect(status().isOk());

        verify(assistantService).chat("hola", null, USER_ID, "ADMIN");
        verify(securityUtils).extractRole(any());
    }

    @Test
    @DisplayName("un prompt vacío se rechaza con 400 y no llega al servicio")
    void rejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/v1/chat")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"   \"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(assistantService);
    }

    @Test
    @DisplayName("el endpoint de explain delega en el servicio con el userId del JWT")
    void explainDelegatesWithUserIdFromJwt() throws Exception {
        when(assistantService.explainCondition("diabetes", "gemini", USER_ID)).thenReturn("Explicación");

        mockMvc.perform(post("/api/v1/chat/explain")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"diabetes\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Explicación"));

        verify(assistantService).explainCondition("diabetes", "gemini", USER_ID);
    }

    @Test
    @DisplayName("el endpoint de symptoms delega en el análisis de síntomas")
    void symptomsDelegatesToAssistantService() throws Exception {
        when(assistantService.analyzeSymptoms("fiebre", "gemini", USER_ID)).thenReturn("Análisis");

        mockMvc.perform(post("/api/v1/chat/symptoms")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"fiebre\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Análisis"));

        verify(assistantService).analyzeSymptoms("fiebre", "gemini", USER_ID);
    }

    @Test
    @DisplayName("el endpoint de diagnose delega en el razonamiento")
    void diagnoseDelegatesToAssistantService() throws Exception {
        when(assistantService.diagnoseWithReasoning("dolor torácico", "gemini", USER_ID)).thenReturn("Razonamiento");

        mockMvc.perform(post("/api/v1/chat/diagnose")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"dolor torácico\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Razonamiento"));

        verify(assistantService).diagnoseWithReasoning("dolor torácico", "gemini", USER_ID);
    }

    @Test
    @DisplayName("el endpoint de consult delega en la consulta general")
    void consultDelegatesToAssistantService() throws Exception {
        when(assistantService.consult("alimentos", "gemini", USER_ID)).thenReturn("Consulta");

        mockMvc.perform(post("/api/v1/chat/consult")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"alimentos\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Consulta"));

        verify(assistantService).consult("alimentos", "gemini", USER_ID);
    }

    @Test
    @DisplayName("el body nunca transporta el userId: sale del JWT")
    void userIdIsNeverTakenFromTheBody() throws Exception {
        when(securityUtils.extractRole(any())).thenReturn("PATIENT");
        when(assistantService.chat(any(), any(), any(), any())).thenReturn("ok");

        mockMvc.perform(post("/api/v1/chat")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"hola\",\"model\":\"gemini\",\"userId\":999}"))
                .andExpect(status().isOk());

        verify(assistantService).chat(eq("hola"), eq("gemini"), eq(USER_ID), any());
    }

    @Test
    @DisplayName("el endpoint de streaming responde como text/event-stream con los chunks del modelo")
    void chatStreamReturnsServerSentEvents() throws Exception {
        when(securityUtils.extractRole(any())).thenReturn("PATIENT");
        when(assistantService.chatStream(any(), any(), any(), any())).thenReturn(Flux.just("Hola ", "mundo"));

        MvcResult result = mockMvc.perform(post("/api/v1/chat/stream")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"hola\",\"model\":\"gemini\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hola")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mundo")));

        verify(assistantService).chatStream("hola", "gemini", USER_ID, "PATIENT");
    }
}
