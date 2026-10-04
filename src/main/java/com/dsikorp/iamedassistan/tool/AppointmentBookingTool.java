package com.dsikorp.iamedassistan.tool;


import com.dsikorp.iamedassistan.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppointmentBookingTool {
    private final AppointmentService appointmentService;

    @Tool(description = "Reservar un turno médico para un paciente. " +
            "Solo personal administrativo puede ejecutar esta acción con role ADMIN. "
            //"Pedir confirmación antes de reservar."
    )
    public String bookAppointment(
            @ToolParam(description = "Especialidad médica" )
            String specialty,
            @ToolParam(description = "Fecha del turno en formato yyyy-MM-dd" )
            String date,
            @ToolParam(description = "Hora del turno en formato HH:mm")
            String time,
            @ToolParam(description = "ID numérico del paciente")
            Long patientId,
            ToolContext context

    ){
        String role = context.getContext().get("role").toString();

        log.info("Tool invocada — bookAppointment: specialty={}, date={}, time={}, patientId={}, role={}",
                specialty, date, time, patientId, role);

        if(!"ADMIN".equals(role)){
            log.warn("Acceso denegado: rol {} intentó reservar turno", role);
            return "Acceso denegado. Solo el personal administrativo puede reservar turnos.";
        }

        log.info("Va a persistir la reserva...");

        // puede generar un DateTimeParseException
        return appointmentService.bookAppointment(specialty, LocalDate.parse(date), LocalTime.parse(time), patientId);

    }
}








