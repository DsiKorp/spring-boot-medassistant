package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentSearchTool")
class AppointmentSearchToolTest {

    private static final String SPECIALTY = "Cardiología";
    private static final String DATE = "2026-01-15";

    @Mock
    private AppointmentService appointmentService;

    private AppointmentSearchTool tool;

    @BeforeEach
    void setUp() {
        tool = new AppointmentSearchTool(appointmentService);
    }

    @Test
    @DisplayName("Delega en el servicio la especialidad y la fecha parseada")
    void searchAppointmentsDelegatesSpecialtyAndParsedDateToService() {
        when(appointmentService.findAvailableAppointments(any(), any())).thenReturn(List.of());

        tool.searchAppointments(SPECIALTY, DATE);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        verify(appointmentService).findAvailableAppointments(eq(SPECIALTY), dateCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("Devuelve la lista de turnos tal cual la devuelve el servicio")
    void searchAppointmentsReturnsServiceListUnchanged() {
        List<AppointmentInfo> expected = List.of(
                new AppointmentInfo("Ana Martínez", "Cardiología", "2026-01-15", "09:00"),
                new AppointmentInfo("Luis Gómez", "Cardiología", "2026-01-15", "10:30")
        );
        when(appointmentService.findAvailableAppointments(SPECIALTY, LocalDate.of(2026, 1, 15)))
                .thenReturn(expected);

        List<AppointmentInfo> result = tool.searchAppointments(SPECIALTY, DATE);

        assertThat(result).isEqualTo(expected);
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("Devuelve una lista vacía cuando no hay turnos disponibles")
    void searchAppointmentsReturnsEmptyListWhenNoAppointmentsAvailable() {
        when(appointmentService.findAvailableAppointments(SPECIALTY, LocalDate.of(2026, 1, 15)))
                .thenReturn(List.of());

        List<AppointmentInfo> result = tool.searchAppointments(SPECIALTY, DATE);

        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Lanza DateTimeParseException con una fecha mal formada sin tocar el servicio")
    void searchAppointmentsThrowsDateTimeParseExceptionForMalformedDate() {
        assertThatThrownBy(() -> tool.searchAppointments(SPECIALTY, "15-01-2026"))
                .isInstanceOf(DateTimeParseException.class);

        verifyNoInteractions(appointmentService);
    }
}
