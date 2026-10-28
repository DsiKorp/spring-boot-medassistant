package com.dsikorp.iamedassistan.mcp;

import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.service.DoctorService;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros de {@link DoctorResourceProvider}: el recurso MCP doctor://{specialty}
 * arma un unico contenido de texto con el listado de doctores.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorResourceProvider")
class DoctorResourceProviderTest {

    @Mock
    private DoctorService doctorService;

    private DoctorResourceProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DoctorResourceProvider(doctorService);
    }

    private static McpSchema.TextResourceContents textContents(McpSchema.ReadResourceResult result) {
        assertThat(result.contents()).hasSize(1);
        McpSchema.ResourceContents contents = result.contents().get(0);
        assertThat(contents).isInstanceOf(McpSchema.TextResourceContents.class);
        return (McpSchema.TextResourceContents) contents;
    }

    @Test
    @DisplayName("Delega la especialidad consultada al servicio de doctores")
    void forwardsSpecialtyToDoctorService() {
        when(doctorService.searchDoctors("cardiología")).thenReturn(List.of());

        provider.getDoctorsBySpecialty("cardiología");

        verify(doctorService, times(1)).searchDoctors("cardiología");
    }

    @Test
    @DisplayName("Informa que no hay doctores cuando la busqueda vuelve vacia")
    void reportsEmptyResultWhenNoDoctorsFound() {
        when(doctorService.searchDoctors("cardiología")).thenReturn(List.of());

        McpSchema.ReadResourceResult result = provider.getDoctorsBySpecialty("cardiología");
        McpSchema.TextResourceContents contents = textContents(result);

        assertThat(contents.text())
                .isEqualTo("No se encontraron doctores para la especialidad: cardiología");
    }

    @Test
    @DisplayName("El recurso usa el uri doctor://<especialidad> y el mime type text/plain")
    void exposesResourceUriAndMimeType() {
        when(doctorService.searchDoctors("cardiología")).thenReturn(List.of());

        McpSchema.ReadResourceResult result = provider.getDoctorsBySpecialty("cardiología");
        McpSchema.TextResourceContents contents = textContents(result);

        assertThat(contents.uri()).isEqualTo("doctor://cardiología");
        assertThat(contents.mimeType()).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("Lista los doctores encontrados uno por linea")
    void listsFoundDoctorsSeparatedByNewline() {
        DoctorInfo first = new DoctorInfo("Ana", "Martínez", "Cardiología", "MP-1", "555", "C-101");
        DoctorInfo second = new DoctorInfo("Carlos", "López", "Cardiología", "MP-2", "556", "C-202");
        when(doctorService.searchDoctors("cardiología")).thenReturn(List.of(first, second));

        McpSchema.ReadResourceResult result = provider.getDoctorsBySpecialty("cardiología");
        McpSchema.TextResourceContents contents = textContents(result);

        assertThat(contents.text())
                .isEqualTo(first.toString() + "\n" + second.toString());
        assertThat(contents.text()).contains(first.toString(), second.toString());
        assertThat(contents.uri()).isEqualTo("doctor://cardiología");
    }
}
