package com.dsikorp.iamedassistan.mcp;


import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.service.DoctorService;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class DoctorResourceProvider {

    private final DoctorService doctorService;

    @McpResource(
            uri = "doctor://{specialty}",  // template
            name = "Doctores por especialidad",
            description = "Lista de doctores disponibles para una especialidad médica"
    )
    public McpSchema.ReadResourceResult getDoctorsBySpecialty(String specialty){
        List<DoctorInfo> doctors = doctorService.searchDoctors(specialty);

        String content = doctors.isEmpty()
                ? "No se encontraron doctores para la especialidad: " + specialty
                : listDoctors(doctors);

        return McpSchema.ReadResourceResult.builder(
                List.of(
                        new McpSchema.TextResourceContents(
                                "doctor://" + specialty,
                                "text/plain", content
                        ))).build();
    }

    //DoctorInfo[firstName=Juan, lastName=Pérez, specialty=cardiología, ...]
    //DoctorInfo[firstName=Juan, lastName=Pérez, specialty=cardiología, ...]
    private String listDoctors(List<DoctorInfo> doctorInfos){
        return doctorInfos.stream()
                .map(DoctorInfo::toString)
                .collect(Collectors.joining("\n"));
    }
}
