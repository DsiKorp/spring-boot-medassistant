package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.service.PatientService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistoryWorkerTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private HistoryWorker worker;

    @Test
    @DisplayName("armá el historial del paciente con sus datos clínicos")
    void buildsPatientHistorySummary() {
        when(patientService.getPatientInfo(7L)).thenReturn(
                new PatientInfo("Gabriel", "Ejemplo", "1985-03-15", "Penicilina", "Hipertensión leve"));

        String result = worker.lookup(7L);

        assertThat(result).isEqualTo(
                "Paciente: Gabriel Ejemplo | Fecha de nacimiento: 1985-03-15 | "
                        + "Alergias: Penicilina | Condiciones: Hipertensión leve");
        verify(patientService).getPatientInfo(7L);
    }

    @Test
    @DisplayName("informa que no hay historial cuando el paciente no existe")
    void reportsMissingPatient() {
        when(patientService.getPatientInfo(99L)).thenReturn(null);

        String result = worker.lookup(99L);

        assertThat(result).isEqualTo("No se encontró historial clínico para este paciente.");
    }

    @Test
    @DisplayName("sustituye alergias y condiciones ausentes por 'ninguna registrada'")
    void handlesMissingAllergiesAndConditions() {
        when(patientService.getPatientInfo(7L)).thenReturn(
                new PatientInfo("Juan", "Demo", "1978-11-08", null, null));

        String result = worker.lookup(7L);

        assertThat(result).isEqualTo(
                "Paciente: Juan Demo | Fecha de nacimiento: 1978-11-08 | "
                        + "Alergias: ninguna registrada | Condiciones: ninguna registrada");
    }
}
