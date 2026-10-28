package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.MockMvcTestSupport;
import com.dsikorp.iamedassistan.agent.AppointmentChainService;
import com.dsikorp.iamedassistan.agent.OpenAgent.OpenAgentService;
import com.dsikorp.iamedassistan.agent.RoutingWorkflowService;
import com.dsikorp.iamedassistan.agent.orchestrator.OrchestratorService;
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

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AppointmentChainService appointmentChainService;

    @Mock
    private RoutingWorkflowService routingWorkflowService;

    @Mock
    private OrchestratorService orchestratorService;

    @Mock
    private OpenAgentService openAgentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.standalone(new AgentController(
                appointmentChainService, routingWorkflowService, orchestratorService, openAgentService)).build();
    }

    @AfterEach
    void tearDown() {
        MockMvcTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("el router delega la consulta con el userId del JWT")
    void routingDelegatesWithUserIdFromJwt() throws Exception {
        when(routingWorkflowService.routeQuery("me duele el pecho", "gemini", USER_ID)).thenReturn("Ruta");

        mockMvc.perform(post("/api/v1/agent/routing")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"me duele el pecho\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Ruta"));

        verify(routingWorkflowService).routeQuery("me duele el pecho", "gemini", USER_ID);
    }

    @Test
    @DisplayName("el orquestador delega con el userId del JWT")
    void orchestratorDelegatesWithUserIdFromJwt() throws Exception {
        when(orchestratorService.orchestrate("fiebre", "gemini", USER_ID)).thenReturn("Síntesis");

        mockMvc.perform(post("/api/v1/agent/orchestrator")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"fiebre\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Síntesis"));

        verify(orchestratorService).orchestrate("fiebre", "gemini", USER_ID);
    }

    @Test
    @DisplayName("la cadena de citas delega el pedido con el userId del JWT")
    void appointmentChainDelegatesWithUserIdFromJwt() throws Exception {
        when(appointmentChainService.bookAppointmentChain("quiero cardiología", "gemini", USER_ID))
                .thenReturn("Turno encontrado");

        mockMvc.perform(post("/api/v1/agent/chain/appointment")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"quiero cardiología\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Turno encontrado"));

        verify(appointmentChainService).bookAppointmentChain("quiero cardiología", "gemini", USER_ID);
    }

    @Test
    @DisplayName("confirmar la reserva usa el userId del JWT y no necesita body")
    void confirmBookingUsesUserIdFromJwt() throws Exception {
        when(appointmentChainService.confirmBooking(USER_ID)).thenReturn("Turno reservado");

        mockMvc.perform(post("/api/v1/agent/chain/appointment/confirm")
                        .with(MockMvcTestSupport.asPatient(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string("Turno reservado"));

        verify(appointmentChainService).confirmBooking(USER_ID);
    }

    @Test
    @DisplayName("cancelar la reserva usa el userId del JWT y no necesita body")
    void cancelBookingUsesUserIdFromJwt() throws Exception {
        when(appointmentChainService.cancelBooking(USER_ID)).thenReturn("Turno descartado");

        mockMvc.perform(post("/api/v1/agent/chain/appointment/cancel")
                        .with(MockMvcTestSupport.asPatient(USER_ID)))
                .andExpect(status().isOk())
                .andExpect(content().string("Turno descartado"));

        verify(appointmentChainService).cancelBooking(USER_ID);
    }

    @Test
    @DisplayName("el agente abierto delega la consulta con el userId del JWT")
    void openAgentDelegatesWithUserIdFromJwt() throws Exception {
        when(openAgentService.execute("¿qué turnos hay?", "gemini", USER_ID)).thenReturn("Respuesta");

        mockMvc.perform(post("/api/v1/agent/open")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"¿qué turnos hay?\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Respuesta"));

        verify(openAgentService).execute("¿qué turnos hay?", "gemini", USER_ID);
    }

    @Test
    @DisplayName("un prompt vacío se rechaza con 400 y no llega a ningún agente")
    void rejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/v1/agent/routing")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(routingWorkflowService, orchestratorService, openAgentService, appointmentChainService);
    }

    @Test
    @DisplayName("el orquestador con progreso emite los marcadores de etapa por SSE")
    void orchestratorProgressStreamsStageMarkers() throws Exception {
        when(orchestratorService.orchestrateWithProgress("fiebre", "gemini", USER_ID))
                .thenReturn(Flux.just(
                        "[PROGRESO] Analizando síntomas clínicamente...",
                        "[PROGRESO] Integrando resultados de los tres especialistas...",
                        "RESPUESTA FINAL"));

        MvcResult result = mockMvc.perform(post("/api/v1/agent/orchestrator/progress")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"fiebre\",\"model\":\"gemini\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("[PROGRESO] Analizando síntomas clínicamente...")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("RESPUESTA FINAL")));

        verify(orchestratorService).orchestrateWithProgress("fiebre", "gemini", USER_ID);
    }
}
