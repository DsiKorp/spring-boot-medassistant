package com.dsikorp.iamedassistan.repository;


import com.dsikorp.iamedassistan.model.Appointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    //WHERE doctor_id IN (1, 3, 5) AND date = '2026-06-23' AND available = true
    List<Appointment> findByDoctorIdInAndDateAndAvailableTrue(
            List<Long> doctorIds, LocalDate date);

    //List<Long> doctorIds, LocalDate date, LocalTime startTime)
    List<Appointment> findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(
            List<Long> doctorIds, LocalDate date, LocalTime startTime);

}
