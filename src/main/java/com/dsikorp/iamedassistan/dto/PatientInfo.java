package com.dsikorp.iamedassistan.dto;

// Tipo de respuesta de la tool
public record PatientInfo(
        String firstName,
        String lastName,
        String dateOfBirth,
        String allergies,
        String conditions
) {
}
