package com.dsikorp.iamedassistan.dto;

public record AppointmentInfo(
        String doctorName,
        String specialty,
        String date,
        String time
) {
}
