package com.dsikorp.iamedassistan.dto;

public record DoctorInfo(
        String firstName,
        String lastName,
        String specialty,
        String licenseNumber,
        String phone,
        String office
) {
}
