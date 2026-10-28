package com.dsikorp.iamedassistan.tools;


import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.service.DoctorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DoctorInfoTool {
    private final DoctorService doctorService;

    @Tool(description = "Buscar información de un médico por nombre o especialidad. " +
            "Usar cuando el usuario pregunte por datos de un doctor, quién atiende una especialidad, " +
            "o información de contacto de un médico.")
    @McpTool(
            name = "search-doctors",
            description = "Busca médicos por nombre y especialidad"
    )
    public List<DoctorInfo> searchDoctors(
            @ToolParam(description = "Solo el apellido del médico o el nombre de " +
                    "la especialidad, sin títulos como Dr. o Dra.")
            @McpToolParam(
                    description = "Apellido o especialidad médica",
                    required = true
            )
            String query) {

        log.info("Tool invocada — searchDoctors: query={}", query);
        return doctorService.searchDoctors(query);
    }
}
