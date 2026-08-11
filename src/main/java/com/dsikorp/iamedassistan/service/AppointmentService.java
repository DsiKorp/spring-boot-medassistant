package com.dsikorp.iamedassistan.service;



import com.dsikorp.iamedassistan.dto.AppointmentInfo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentService {
    List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date);
    String bookAppointment(String specialty, LocalDate date, LocalTime time, Long patientId);
}
