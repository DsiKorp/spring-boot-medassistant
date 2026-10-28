package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.MockMvcTestSupport;
import com.dsikorp.iamedassistan.dto.analysis.ConditionSummaryDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryTypeEnum;
import com.dsikorp.iamedassistan.dto.analysis.SeverityEnum;
import com.dsikorp.iamedassistan.dto.analysis.SymptomAnalysisDto;
import com.dsikorp.iamedassistan.service.AnalysisService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    private static final Long USER_ID = 7L;

    @Mock
    private AnalysisService analysisService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.standalone(new AnalysisController(analysisService)).build();
    }

    @AfterEach
    void tearDown() {
        MockMvcTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("el resumen de una condición se devuelve como DTO tipado")
    void conditionSummaryIsReturnedAsTypedDto() throws Exception {
        when(analysisService.summarizeCondition("asma bronquial", "gemini", USER_ID)).thenReturn(
                new ConditionSummaryDto("Asma bronquial", "Inflamación de los bronquios",
                        "Tos, disnea", SeverityEnum.MODERATE, "Ante difficulty respiratoria"));

        mockMvc.perform(post("/api/v1/analysis/condition")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"asma bronquial\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conditionName").value("Asma bronquial"))
                .andExpect(jsonPath("$.severity").value("MODERATE"));

        verify(analysisService).summarizeCondition("asma bronquial", "gemini", USER_ID);
    }

    @Test
    @DisplayName("las condiciones relacionadas se devuelven como lista")
    void relatedConditionsAreReturnedAsList() throws Exception {
        when(analysisService.listRelatedConditions("EPOC", "gemini", USER_ID)).thenReturn(List.of(
                new ConditionSummaryDto("EPOC", "Descripción", "Síntomas", SeverityEnum.SEVERE, "Consultar"),
                new ConditionSummaryDto("Bronquitis", "Descripción", "Síntomas", SeverityEnum.MILD, "Consultar")));

        mockMvc.perform(post("/api/v1/analysis/conditions")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"EPOC\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].conditionName").value("EPOC"));
    }

    @Test
    @DisplayName("el análisis de síntomas se devuelve como DTO tipado")
    void symptomAnalysisIsReturnedAsTypedDto() throws Exception {
        when(analysisService.analyzeSymptoms("cefalea", "gemini", USER_ID)).thenReturn(
                new SymptomAnalysisDto(List.of("cefalea"), List.of(), null, "Consultar"));

        mockMvc.perform(post("/api/v1/analysis/symptoms")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"cefalea\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.identifiedSymptoms[0]").value("cefalea"))
                .andExpect(jsonPath("$.recommendation").value("Consultar"));
    }

    @Test
    @DisplayName("la clasificación devuelve el tipo y la razón de la consulta")
    void classificationIsReturnedAsTypedDto() throws Exception {
        when(analysisService.classifyQuery("me duele el pecho", "gemini", USER_ID)).thenReturn(
                new QueryClassificationDto(QueryTypeEnum.EMERGENCY, "síntoma potencialmente urgente"));

        mockMvc.perform(post("/api/v1/analysis/classify")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"me duele el pecho\",\"model\":\"gemini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EMERGENCY"))
                .andExpect(jsonPath("$.reason").value("síntoma potencialmente urgente"));
    }

    @Test
    @DisplayName("un prompt vacío se rechaza con 400 y no llega al servicio")
    void rejectsBlankPrompt() throws Exception {
        mockMvc.perform(post("/api/v1/analysis/condition")
                        .with(MockMvcTestSupport.asPatient(USER_ID))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(analysisService);
    }

    @Test
    @DisplayName("el userId del análisis sale del claim del JWT")
    void userIdComesFromJwtClaim() throws Exception {
        when(analysisService.classifyQuery(any(), any(), any()))
                .thenReturn(new QueryClassificationDto(QueryTypeEnum.GENERAL_QUESTION, "motivo"));

        mockMvc.perform(post("/api/v1/analysis/classify")
                        .with(MockMvcTestSupport.asPatient(42L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"prompt\":\"¿qué es la diabetes?\"}"))
                .andExpect(status().isOk());

        verify(analysisService).classifyQuery("¿qué es la diabetes?", null, 42L);
    }
}
