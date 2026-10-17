package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class HistoryWorker {

    private final PatientService patientService;

    public String lookup(Long userId){

        PatientInfo patient = patientService.getPatientInfo(userId);

        log.info("Looking up patient {}", patient);

        if(patient==null){
            log.info("Worker historial — paciente no encontrado");
            return "No se encontró historial clínico para este paciente.";
        }

        String result = String.format(
                "Paciente: %s %s | Fecha de nacimiento: %s | Alergias: %s | Condiciones: %s",
                patient.firstName(), patient.lastName(),
                patient.dateOfBirth(),
                patient.allergies() != null ? patient.allergies() : "ninguna registrada",
                patient.conditions() != null ? patient.conditions() : "ninguna registrada"
        );

        log.info("Worker historial — historial encontrado");

        return result;
    }

}














