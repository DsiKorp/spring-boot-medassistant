package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.model.Appointment;
import com.dsikorp.iamedassistan.model.Doctor;
import com.dsikorp.iamedassistan.repository.AppointmentRepository;
import com.dsikorp.iamedassistan.repository.DoctorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link AppointmentServiceImpl}.
 * No se levanta contexto de Spring: los repositorios se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AppointmentServiceImpl")
class AppointmentServiceImplTest {

    private static final LocalDate FECHA = LocalDate.of(2026, 6, 23);
    private static final LocalTime HORA = LocalTime.of(9, 30);

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private AppointmentRepository appointmentRepository;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private static Doctor doctor(Long id, String firstName, String lastName) {
        return Doctor.builder()
                .id(id)
                .firstName(firstName)
                .lastName(lastName)
                .specialty("Cardiología")
                .licenseNumber("MP-" + id)
                .phone("555")
                .office("C-" + id)
                .build();
    }

    private static Appointment turno(Long id, Long doctorId, boolean available) {
        return Appointment.builder()
                .id(id)
                .doctorId(doctorId)
                .date(FECHA)
                .startTime(HORA)
                .available(available)
                .build();
    }

    // ------------------------------------------------------------------
    // findAvailableAppointments
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findAvailableAppointments devuelve lista vacía cuando no hay médicos con esa especialidad")
    void findAvailableAppointmentsReturnsEmptyListWhenNoDoctorHasTheSpecialty() {

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología")).thenReturn(List.of());

        List<AppointmentInfo> resultado = appointmentService.findAvailableAppointments("Cardiología", FECHA);

        assertThat(resultado).isEmpty();
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("findAvailableAppointments devuelve lista vacía cuando hay médicos pero ningún turno disponible")
    void findAvailableAppointmentsReturnsEmptyListWhenThereAreNoAvailableAppointments() {

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología"))
                .thenReturn(List.of(doctor(1L, "Ana", "Martínez")));

        when(appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(anyList(), eq(FECHA)))
                .thenReturn(List.of());

        List<AppointmentInfo> resultado = appointmentService.findAvailableAppointments("Cardiología", FECHA);

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("findAvailableAppointments mapea los turnos a AppointmentInfo con nombre, especialidad, fecha y hora")
    void findAvailableAppointmentsMapsAppointmentsToAppointmentInfo() {

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología"))
                .thenReturn(List.of(doctor(1L, "Ana", "Martínez")));

        when(appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(anyList(), eq(FECHA)))
                .thenReturn(List.of(turno(10L, 1L, true)));

        List<AppointmentInfo> resultado = appointmentService.findAvailableAppointments("Cardiología", FECHA);

        assertThat(resultado).hasSize(1);

        AppointmentInfo info = resultado.getFirst();
        assertThat(info.doctorName()).isEqualTo("Ana Martínez");
        assertThat(info.specialty()).isEqualTo("Cardiología");
        assertThat(info.date()).isEqualTo("2026-06-23");
        assertThat(info.time()).isEqualTo("09:30");
    }

    @Test
    @DisplayName("findAvailableAppointments consulta el repositorio de turnos con los ids de los médicos encontrados")
    void findAvailableAppointmentsQueriesExactlyTheIdsOfTheMatchingDoctors() {

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología"))
                .thenReturn(List.of(doctor(1L, "Ana", "Martínez"), doctor(2L, "Luis", "Gómez")));

        when(appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(anyList(), eq(FECHA)))
                .thenReturn(List.of());

        appointmentService.findAvailableAppointments("Cardiología", FECHA);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> doctorIdsCaptor = ArgumentCaptor.forClass(List.class);

        verify(appointmentRepository)
                .findByDoctorIdInAndDateAndAvailableTrue(doctorIdsCaptor.capture(), eq(FECHA));

        assertThat(doctorIdsCaptor.getValue()).containsExactlyInAnyOrder(1L, 2L);
    }

    // ------------------------------------------------------------------
    // bookAppointment
    // ------------------------------------------------------------------

    @Test
    @DisplayName("bookAppointment informa que no encontró la especialidad y no guarda nada")
    void bookAppointmentFailsWhenSpecialtyIsUnknown() {

        when(doctorRepository.findBySpecialtyIgnoreCase("cardiología")).thenReturn(List.of());

        String respuesta = appointmentService.bookAppointment("cardiología", FECHA, HORA, 77L);

        assertThat(respuesta).isEqualTo("No se encontró la especialidad: cardiología");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("bookAppointment informa que el turno no está disponible y no guarda nada")
    void bookAppointmentFailsWhenNoAppointmentIsAvailable() {

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología"))
                .thenReturn(List.of(doctor(1L, "Ana", "Martínez")));

        when(appointmentRepository
                .findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List.of(1L), FECHA, HORA))
                .thenReturn(List.of());

        String respuesta = appointmentService.bookAppointment("Cardiología", FECHA, HORA, 77L);

        assertThat(respuesta).isEqualTo("El turno no está disponible.");
        verify(appointmentRepository, never()).save(any(Appointment.class));
    }

    @Test
    @DisplayName("bookAppointment reserva el turno, lo marca como no disponible y asigna el paciente")
    void bookAppointmentReservesTheTurnAndMarksItAsUnavailable() {

        Appointment disponible = turno(10L, 1L, true);

        when(doctorRepository.findBySpecialtyIgnoreCase("Cardiología"))
                .thenReturn(List.of(doctor(1L, "Ana", "Martínez")));

        when(appointmentRepository
                .findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(List.of(1L), FECHA, HORA))
                .thenReturn(List.of(disponible));

        String respuesta = appointmentService.bookAppointment("Cardiología", FECHA, HORA, 77L);

        assertThat(respuesta).startsWith("Turno reservado exitosamente.");
        assertThat(respuesta).contains("2026-06-23");
        assertThat(respuesta).contains("09:30");

        ArgumentCaptor<Appointment> turnoCaptor = ArgumentCaptor.forClass(Appointment.class);

        verify(appointmentRepository).save(turnoCaptor.capture());

        Appointment guardado = turnoCaptor.getValue();
        assertThat(guardado.isAvailable()).isFalse();
        assertThat(guardado.getPatientId()).isEqualTo(77L);
        assertThat(guardado.getDate()).isEqualTo(FECHA);
        assertThat(guardado.getStartTime()).isEqualTo(HORA);
    }
}
