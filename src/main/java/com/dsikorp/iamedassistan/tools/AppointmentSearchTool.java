package com.dsikorp.iamedassistan.tools;

import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentSearchTool {
    private final AppointmentService appointmentService;

    @Tool(description = "Buscar turnos médicos disponibles para una especialidad y fecha. Usar cuando el " +
            "usuario pregunte por disponibilidad de turnos o citas médicas.")
    @McpTool(name = "search-appointments",
            description = "Busca turnos disponibles para una especialidad en una fecha")
    public List<AppointmentInfo> searchAppointments(
            @ToolParam(description = "Especialidad médica, por ejemplo: cardiología, pediatría, dermatología")
            @McpToolParam(description = "Especialidad médica", required = true)
            String specialty,
            @ToolParam(description = "Fecha de la cita en formato yyyy-MM-dd")
            @McpToolParam(description = "Fecha en formato yyyy-MM-dd", required = true)
            String date
    ){
        log.info("Tool invocada — searchAppointments: specialty={}, date={}", specialty, date);

        return appointmentService.findAvailableAppointments(specialty, LocalDate.parse(date));
    }
}













