package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.dto.DrugInfo;
import com.dsikorp.iamedassistan.service.DrugInfoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DrugInfoTool")
class DrugInfoToolTest {

    @Mock
    private DrugInfoService drugInfoService;

    private DrugInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new DrugInfoTool(drugInfoService);
    }

    @Test
    @DisplayName("Envía el nombre del medicamento al servicio tal cual se recibió")
    void getDrugInfoForwardsDrugNameVerbatimToService() {
        when(drugInfoService.getDrugInfo("ibuprofen")).thenReturn(null);

        tool.getDrugInfo("ibuprofen");

        ArgumentCaptor<String> drugNameCaptor = ArgumentCaptor.forClass(String.class);
        verify(drugInfoService).getDrugInfo(drugNameCaptor.capture());
        assertThat(drugNameCaptor.getValue()).isEqualTo("ibuprofen");
    }

    @Test
    @DisplayName("Devuelve la información del medicamento tal cual la devuelve el servicio")
    void getDrugInfoReturnsServiceResultUnchanged() {
        DrugInfo expected = new DrugInfo("Advil", "ibuprofen", "Analgésico y antiinflamatorio",
                "No usar en caso de alergia", "200 mg cada 6-8 horas");
        when(drugInfoService.getDrugInfo("ibuprofen")).thenReturn(expected);

        DrugInfo result = tool.getDrugInfo("ibuprofen");

        assertThat(result).isEqualTo(expected);
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Devuelve null cuando el servicio no encuentra el medicamento")
    void getDrugInfoReturnsNullWhenServiceReturnsNull() {
        when(drugInfoService.getDrugInfo("medicamento-inexistente")).thenReturn(null);

        DrugInfo result = tool.getDrugInfo("medicamento-inexistente");

        assertThat(result).isNull();
    }
}
