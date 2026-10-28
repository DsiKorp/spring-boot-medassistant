package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.service.AppointmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ToolContext;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentBookingTool")
class AppointmentBookingToolTest {

    private static final String DENIED_MESSAGE =
            "Acceso denegado. Solo el personal administrativo puede reservar turnos.";

    private static final String SPECIALTY = "Cardiología";
    private static final String DATE = "2026-01-15";
    private static final String TIME = "09:00";
    private static final Long PATIENT_ID = 7L;

    @Mock
    private AppointmentService appointmentService;

    private AppointmentBookingTool tool;

    @BeforeEach
    void setUp() {
        tool = new AppointmentBookingTool(appointmentService);
    }

    @Test
    @DisplayName("Reserva el turno cuando el rol es ADMIN")
    void bookAppointmentDelegatesToServiceForAdminRole() {
        when(appointmentService.bookAppointment(any(), any(), any(), any()))
                .thenReturn("Turno reservado con éxito");
        ToolContext context = new ToolContext(Map.of("role", "ADMIN"));

        String result = tool.bookAppointment(SPECIALTY, DATE, TIME, PATIENT_ID, context);

        assertThat(result).isEqualTo("Turno reservado con éxito");
    }

    @Test
    @DisplayName("Envía al servicio la especialidad, la fecha, la hora y el paciente")
    void bookAppointmentSendsSpecialtyDateTimeAndPatientIdToService() {
        when(appointmentService.bookAppointment(any(), any(), any(), any()))
                .thenReturn("Turno reservado con éxito");
        ToolContext context = new ToolContext(Map.of("role", "ADMIN"));

        tool.bookAppointment(SPECIALTY, DATE, TIME, PATIENT_ID, context);

        ArgumentCaptor<LocalDate> dateCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalTime> timeCaptor = ArgumentCaptor.forClass(LocalTime.class);
        ArgumentCaptor<Long> patientIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(appointmentService).bookAppointment(eq(SPECIALTY), dateCaptor.capture(),
                timeCaptor.capture(), patientIdCaptor.capture());
        assertThat(dateCaptor.getValue()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(timeCaptor.getValue()).isEqualTo(LocalTime.of(9, 0));
        assertThat(patientIdCaptor.getValue()).isEqualTo(PATIENT_ID);
    }

    @Test
    @DisplayName("Deniega la reserva y no llama al servicio cuando el rol es PATIENT")
    void bookAppointmentDeniesAccessForPatientRole() {
        ToolContext context = new ToolContext(Map.of("role", "PATIENT"));

        String result = tool.bookAppointment(SPECIALTY, DATE, TIME, PATIENT_ID, context);

        assertThat(result).isEqualTo(DENIED_MESSAGE);
        verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Deniega la reserva si el rol ADMIN viene en minúsculas (comparación sensible a mayúsculas)")
    void bookAppointmentDeniesAccessForLowercaseAdminRole() {
        ToolContext context = new ToolContext(Map.of("role", "admin"));

        String result = tool.bookAppointment(SPECIALTY, DATE, TIME, PATIENT_ID, context);

        assertThat(result).isEqualTo(DENIED_MESSAGE);
        verifyNoInteractions(appointmentService);
    }
}
