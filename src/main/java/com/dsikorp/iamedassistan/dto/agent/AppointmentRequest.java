package com.dsikorp.iamedassistan.dto.agent;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record AppointmentRequest(
        @JsonPropertyDescription("Especialidad médica solicitada, por ejemplo: cardiología, pediatría, dermatología")
        String specialty,

        @JsonPropertyDescription("Fecha solicitada en formato yyyy-MM-dd")
        String date
) {
}
