package com.dsikorp.iamedassistan.dto;

// Tipo de respuesta de la tool
public record AppointmentInfo(
        String doctorName,
        String specialty,
        String date,
        String time
) {
}
