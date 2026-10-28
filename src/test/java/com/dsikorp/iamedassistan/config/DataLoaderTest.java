package com.dsikorp.iamedassistan.config;

import com.dsikorp.iamedassistan.model.Appointment;
import com.dsikorp.iamedassistan.model.Doctor;
import com.dsikorp.iamedassistan.model.Patient;
import com.dsikorp.iamedassistan.repository.AppointmentRepository;
import com.dsikorp.iamedassistan.repository.DoctorRepository;
import com.dsikorp.iamedassistan.repository.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros de {@link DataLoader}: verifica la guarda de idempotencia
 * y el contenido del seed inicial (doctores, turnos y pacientes).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DataLoader")
class DataLoaderTest {

    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private AppointmentRepository appointmentRepository;
    @Mock
    private PatientRepository patientRepository;

    private DataLoader dataLoader;

    @BeforeEach
    void setUp() {
        dataLoader = new DataLoader(doctorRepository, appointmentRepository, patientRepository);
    }

    @SuppressWarnings("unchecked")
    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        return (ArgumentCaptor<List<T>>) (ArgumentCaptor<?>) ArgumentCaptor.forClass(List.class);
    }

    private static Doctor doctor(long id, String specialty) {
        return Doctor.builder()
                .id(id)
                .firstName("Doctor " + id)
                .lastName("Apellido")
                .specialty(specialty)
                .licenseNumber("MP-" + id)
                .phone("555-000" + id)
                .office("Consultorio " + id)
                .build();
    }

    @Test
    @DisplayName("No siembra nada si ya hay doctores cargados")
    void doesNotSeedWhenDoctorsAlreadyExist() {
        when(doctorRepository.count()).thenReturn(5L);

        dataLoader.run();

        verify(doctorRepository, never()).saveAll(any());
        verify(appointmentRepository, never()).save(any(Appointment.class));
        verify(patientRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Siembra cinco doctores en una instalacion nueva")
    void seedsFiveDoctorsOnFreshInstall() {
        when(doctorRepository.count()).thenReturn(0L);
        when(doctorRepository.findAll()).thenReturn(List.of(
                doctor(1L, "Cardiología"),
                doctor(2L, "Dermatología"),
                doctor(3L, "Pediatría"),
                doctor(4L, "Traumatología"),
                doctor(5L, "Endocrinología")));

        dataLoader.run();

        ArgumentCaptor<List<Doctor>> captor = listCaptor();
        verify(doctorRepository, times(1)).saveAll(captor.capture());

        List<Doctor> savedDoctors = captor.getValue();
        assertThat(savedDoctors).hasSize(5);
        assertThat(savedDoctors).extracting(Doctor::getSpecialty)
                .containsExactlyInAnyOrder("Cardiología", "Dermatología", "Pediatría", "Traumatología", "Endocrinología");
        assertThat(savedDoctors).extracting(Doctor::getFirstName)
                .contains("Ana", "Carlos", "Laura", "Martín", "Sofía");
    }

    @Test
    @DisplayName("Siembra tres pacientes en una instalacion nueva")
    void seedsThreePatientsOnFreshInstall() {
        when(doctorRepository.count()).thenReturn(0L);
        when(doctorRepository.findAll()).thenReturn(List.of(
                doctor(1L, "Cardiología"),
                doctor(2L, "Dermatología"),
                doctor(3L, "Pediatría"),
                doctor(4L, "Traumatología"),
                doctor(5L, "Endocrinología")));

        dataLoader.run();

        ArgumentCaptor<List<Patient>> captor = listCaptor();
        verify(patientRepository, times(1)).saveAll(captor.capture());

        List<Patient> savedPatients = captor.getValue();
        assertThat(savedPatients).hasSize(3);
        assertThat(savedPatients).extracting(Patient::getFirstName)
                .containsExactlyInAnyOrder("Gabriel", "María", "Juan");
        assertThat(savedPatients).extracting(Patient::getAllergies)
                .containsExactlyInAnyOrder("Penicilina", "Ninguna conocida", "Ibuprofeno, Mariscos");
    }

    @Test
    @DisplayName("Siembra veinte turnos en una instalacion nueva")
    void seedsTwentyAppointmentsOnFreshInstall() {
        when(doctorRepository.count()).thenReturn(0L);
        when(doctorRepository.findAll()).thenReturn(List.of(
                doctor(1L, "Cardiología"),
                doctor(2L, "Dermatología"),
                doctor(3L, "Pediatría"),
                doctor(4L, "Traumatología"),
                doctor(5L, "Endocrinología")));

        LocalDate todayBeforeRun = LocalDate.now();
        dataLoader.run();

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(20)).save(captor.capture());

        List<Appointment> appointments = captor.getAllValues();
        assertThat(appointments).hasSize(20);
        assertThat(appointments).extracting(Appointment::getDate).doesNotContainNull();
        assertThat(appointments).allSatisfy(appointment ->
                assertThat(appointment.getDate()).isAfter(todayBeforeRun));
        assertThat(appointments.stream().map(Appointment::getDate).distinct().toList()).hasSize(5);
        assertThat(appointments).extracting(Appointment::getStartTime)
                .allMatch(time -> Set.of(
                        LocalTime.of(9, 0),
                        LocalTime.of(10, 0),
                        LocalTime.of(11, 0),
                        LocalTime.of(15, 0)).contains(time));
        assertThat(appointments).extracting(Appointment::getDoctorId)
                .doesNotContainNull()
                .isSubsetOf(Set.of(1L, 2L, 3L, 4L, 5L));
    }

    @Test
    @DisplayName("Los turnos alternan disponibilidad segun el seed original")
    void seededAppointmentsKeepAvailabilityPattern() {
        when(doctorRepository.count()).thenReturn(0L);
        when(doctorRepository.findAll()).thenReturn(List.of(
                doctor(1L, "Cardiología"),
                doctor(2L, "Dermatología"),
                doctor(3L, "Pediatría"),
                doctor(4L, "Traumatología"),
                doctor(5L, "Endocrinología")));

        dataLoader.run();

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(20)).save(captor.capture());

        List<Appointment> appointments = captor.getAllValues();
        assertThat(appointments).filteredOn(appointment -> !appointment.isAvailable()).hasSize(6);
        assertThat(appointments).filteredOn(Appointment::isAvailable).hasSize(14);
    }

    @Test
    @DisplayName("Los turnos se reparten entre los cinco doctores")
    void seededAppointmentsAreDistributedAmongDoctors() {
        when(doctorRepository.count()).thenReturn(0L);
        when(doctorRepository.findAll()).thenReturn(List.of(
                doctor(1L, "Cardiología"),
                doctor(2L, "Dermatología"),
                doctor(3L, "Pediatría"),
                doctor(4L, "Traumatología"),
                doctor(5L, "Endocrinología")));

        dataLoader.run();

        ArgumentCaptor<Appointment> captor = ArgumentCaptor.forClass(Appointment.class);
        verify(appointmentRepository, times(20)).save(captor.capture());

        List<Appointment> appointments = captor.getAllValues();
        assertThat(appointments).extracting(Appointment::getDoctorId)
                .containsOnly(1L, 2L, 3L, 4L, 5L);
        assertThat(appointments.stream()
                .filter(appointment -> appointment.getDoctorId() == 1L))
                .hasSize(4);
    }
}
