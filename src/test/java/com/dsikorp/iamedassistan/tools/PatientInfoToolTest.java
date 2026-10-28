package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.service.PatientService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PatientInfoTool")
class PatientInfoToolTest {

    @Mock
    private PatientService patientService;

    private PatientInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new PatientInfoTool(patientService);
    }

    @Test
    @DisplayName("Devuelve el historial cuando el userId coincide con el patientId solicitado")
    void getPatientInfoReturnsPatientWhenUserIdMatchesPatientId() {
        PatientInfo expected = new PatientInfo("Ana", "Martínez", "1990-04-12", "Penicilina", "Hipertensión");
        when(patientService.getPatientInfo(7L)).thenReturn(expected);
        ToolContext context = new ToolContext(Map.of("userId", 7L));

        PatientInfo result = tool.getPatientInfo(7L, context);

        assertThat(result).isEqualTo(expected);
        verify(patientService).getPatientInfo(7L);
    }

    @Test
    @DisplayName("Devuelve null y no consulta el servicio si el userId es distinto al patientId")
    void getPatientInfoReturnsNullWhenUserIdDiffersFromPatientId() {
        ToolContext context = new ToolContext(Map.of("userId", 7L));

        PatientInfo result = tool.getPatientInfo(8L, context);

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }

    @Test
    @DisplayName("Devuelve null y no consulta el servicio si el contexto no trae userId")
    void getPatientInfoReturnsNullWhenContextHasNoUserId() {
        ToolContext context = new ToolContext(Map.<String, Object>of());

        PatientInfo result = tool.getPatientInfo(7L, context);

        assertThat(result).isNull();
        verifyNoInteractions(patientService);
    }
}
