package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.mapper.PatientInfoMapper;
import com.dsikorp.iamedassistan.model.Patient;
import com.dsikorp.iamedassistan.repository.PatientRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link PatientServiceImpl}.
 * No se levanta contexto de Spring: el repositorio y el mapper se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PatientServiceImpl")
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private PatientInfoMapper patientInfoMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    @Test
    @DisplayName("getPatientInfo devuelve el DTO producido por el mapper cuando el paciente existe")
    void getPatientInfoReturnsTheMappedDtoWhenThePatientExists() {

        Patient paciente = Patient.builder()
                .id(7L)
                .firstName("María")
                .lastName("López")
                .dateOfBirth(LocalDate.of(1985, 4, 12))
                .allergies("Penicilina")
                .conditions("Hipertensión")
                .build();

        PatientInfo esperado = new PatientInfo(
                "María", "López", "1985-04-12", "Penicilina", "Hipertensión");

        when(patientRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(patientInfoMapper.toPatientInfo(paciente)).thenReturn(esperado);

        PatientInfo resultado = patientService.getPatientInfo(7L);

        assertThat(resultado).isNotNull().isEqualTo(esperado);
    }

    @Test
    @DisplayName("getPatientInfo devuelve null cuando el paciente no existe")
    void getPatientInfoReturnsNullWhenThePatientDoesNotExist() {

        when(patientRepository.findById(404L)).thenReturn(Optional.empty());

        PatientInfo resultado = patientService.getPatientInfo(404L);

        assertThat(resultado).isNull();
    }

    @Test
    @DisplayName("getPatientInfo consulta el repositorio con el id recibido")
    void getPatientInfoQueriesTheRepositoryWithTheGivenId() {

        when(patientRepository.findById(99L)).thenReturn(Optional.empty());

        patientService.getPatientInfo(99L);

        verify(patientRepository).findById(99L);
    }
}
