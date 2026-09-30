package com.dsikorp.iamedassistan.tool;


import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PatientInfoTool {
    private final PatientService patientService;

    @Tool(description = "Consultar el historial clínico de un paciente por su ID. " +
            "Usar cuando el usuario pregunte por su historial médico, alergias o condiciones.")
    public PatientInfo getPatientInfo(
            @ToolParam(description = "ID numérico del paciente") Long patientId,
            ToolContext context
    ){
        log.info("Tool invocada — getPatientInfo: patientId={}", patientId);

        Long userId = (Long) context.getContext().get("userId");
        log.info("ToolContext — userId={}", userId);

        if(!patientId.equals(userId)){
            log.warn("Acceso denegado: userId={} intentó acceder a patientId={}", userId, patientId);
            return null;
        }

        return patientService.getPatientInfo(patientId);
    }
}














