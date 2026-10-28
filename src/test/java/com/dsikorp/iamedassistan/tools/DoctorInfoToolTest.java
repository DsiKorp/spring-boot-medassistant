package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.service.DoctorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorInfoTool")
class DoctorInfoToolTest {

    @Mock
    private DoctorService doctorService;

    private DoctorInfoTool tool;

    @BeforeEach
    void setUp() {
        tool = new DoctorInfoTool(doctorService);
    }

    @Test
    @DisplayName("Envía la consulta al servicio tal cual se recibió")
    void searchDoctorsForwardsQueryVerbatimToService() {
        when(doctorService.searchDoctors("Martínez")).thenReturn(List.of());

        tool.searchDoctors("Martínez");

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(doctorService).searchDoctors(queryCaptor.capture());
        assertThat(queryCaptor.getValue()).isEqualTo("Martínez");
    }

    @Test
    @DisplayName("Devuelve la lista de médicos tal cual la devuelve el servicio")
    void searchDoctorsReturnsServiceListUnchanged() {
        List<DoctorInfo> expected = List.of(
                new DoctorInfo("Ana", "Martínez", "Cardiología", "MP-1", "555-0101", "C-101"),
                new DoctorInfo("Luis", "Gómez", "Pediatría", "MP-2", "555-0102", "C-102")
        );
        when(doctorService.searchDoctors("Cardiología")).thenReturn(expected);

        List<DoctorInfo> result = tool.searchDoctors("Cardiología");

        assertThat(result).isEqualTo(expected);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando el servicio no encuentra médicos")
    void searchDoctorsReturnsEmptyListWhenNoMatches() {
        when(doctorService.searchDoctors("Dermatología")).thenReturn(List.of());

        List<DoctorInfo> result = tool.searchDoctors("Dermatología");

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }
}
