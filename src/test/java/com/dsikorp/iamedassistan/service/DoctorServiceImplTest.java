package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.mapper.DoctorInfoMapper;
import com.dsikorp.iamedassistan.model.Doctor;
import com.dsikorp.iamedassistan.repository.DoctorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de {@link DoctorServiceImpl}.
 * No se levanta contexto de Spring: el repositorio y el mapper se simulan con Mockito.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DoctorServiceImpl")
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorInfoMapper doctorInfoMapper;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    @Test
    @DisplayName("searchDoctors devuelve los DTOs producidos por el mapper respetando el orden del repositorio")
    void searchDoctorsReturnsMappedDtosInRepositoryOrder() {

        Doctor primero = Doctor.builder()
                .id(1L)
                .firstName("Ana")
                .lastName("Martínez")
                .specialty("Cardiología")
                .licenseNumber("MP-1")
                .phone("555")
                .office("C-1")
                .build();

        Doctor segundo = Doctor.builder()
                .id(2L)
                .firstName("Luis")
                .lastName("Gómez")
                .specialty("Pediatría")
                .licenseNumber("MP-2")
                .phone("556")
                .office("C-2")
                .build();

        DoctorInfo primeroDto = new DoctorInfo("Ana", "Martínez", "Cardiología", "MP-1", "555", "C-1");
        DoctorInfo segundoDto = new DoctorInfo("Luis", "Gómez", "Pediatría", "MP-2", "556", "C-2");

        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "ana", "ana", "ana"))
                .thenReturn(List.of(primero, segundo));

        when(doctorInfoMapper.toDoctorInfo(primero)).thenReturn(primeroDto);
        when(doctorInfoMapper.toDoctorInfo(segundo)).thenReturn(segundoDto);

        List<DoctorInfo> resultado = doctorService.searchDoctors("ana");

        assertThat(resultado)
                .hasSize(2)
                .containsExactly(primeroDto, segundoDto);
    }

    @Test
    @DisplayName("searchDoctors devuelve una lista vacía cuando el repositorio no encuentra médicos")
    void searchDoctorsReturnsEmptyListWhenNoDoctorMatches() {

        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        "zzz", "zzz", "zzz"))
                .thenReturn(List.of());

        List<DoctorInfo> resultado = doctorService.searchDoctors("zzz");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("searchDoctors consulta el repositorio con la misma búsqueda para nombre, apellido y especialidad")
    void searchDoctorsPassesTheSameQueryForFirstNameLastNameAndSpecialty() {

        String query = "Cardio";

        when(doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        query, query, query))
                .thenReturn(List.of());

        doctorService.searchDoctors(query);

        verify(doctorRepository)
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        query, query, query);
    }
}
