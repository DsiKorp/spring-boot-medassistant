package com.dsikorp.iamedassistan.dto.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AppointmentSelection(
        @JsonPropertyDescription("Especialidad médica del turno seleccionado")
        String specialty,

        @JsonPropertyDescription("Fecha del turno seleccionado en formato yyyy-MM-dd")
        String date,

        @JsonPropertyDescription("Hora del turno seleccionado en formato HH:mm")
        String time,

        @JsonPropertyDescription("Nombre del médico del turno seleccionado")
        String doctorName,

        @JsonPropertyDescription("Mensaje breve informando al paciente sobre el turno encontrado")
        String message
) {
}