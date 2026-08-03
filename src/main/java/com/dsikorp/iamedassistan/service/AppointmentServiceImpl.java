package com.dsikorp.iamedassistan.service;


import com.dsikorp.iamedassistan.dto.AppointmentInfo;
import com.dsikorp.iamedassistan.model.Appointment;
import com.dsikorp.iamedassistan.model.Doctor;
import com.dsikorp.iamedassistan.repository.AppointmentRepository;
import com.dsikorp.iamedassistan.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentServiceImpl implements AppointmentService {

    private final DoctorRepository doctorRepository;
    private final AppointmentRepository appointmentRepository;

    @Override
    public List<AppointmentInfo> findAvailableAppointments(String specialty, LocalDate date){
        log.info("Buscando turnos disponibles: specialty={}, date={}", specialty, date);

            var doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);

            if(doctors.isEmpty()){
                log.info("No se encontraron médicos con la especialidad: {}", specialty);
                return List.of();
            }

            var doctorNames = buildDoctorNameMap(doctors);
            var doctorIds = new ArrayList<>(doctorNames.keySet());
            var appointments = appointmentRepository.findByDoctorIdInAndDateAndAvailableTrue(doctorIds, date);

            return toAppointmentInfoList(appointments, doctorNames, specialty);
    }

    @Transactional
    @Override
    public String bookAppointment(String specialty, LocalDate date, LocalTime time, Long patientId) {

        log.info("Reservando turno: specialty={}, date={}, time={}, patientId={}",
                specialty, date, time, patientId);

        var doctors = doctorRepository.findBySpecialtyIgnoreCase(specialty);
        if(doctors.isEmpty()){
            return "No se encontró la especialidad: " + specialty;
        }

        var doctorIds = doctors.stream().map(Doctor::getId).toList();
        var appointment = findAvailableAppointment(doctorIds, date, time);
        if(appointment==null){
            return "El turno no está disponible.";
        }

        appointment.setAvailable(false);
        appointment.setPatientId(patientId);

        appointmentRepository.save(appointment);

        return "Turno reservado exitosamente.";
    }

    @Transactional(readOnly = true)
    private Appointment findAvailableAppointment(List<Long> doctorIds, LocalDate date, LocalTime time){
        return appointmentRepository.findByDoctorIdInAndDateAndStartTimeAndAvailableTrue(doctorIds,date,time)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private Map<Long, String> buildDoctorNameMap(List<Doctor> doctors){
        return doctors.stream()
                .collect(Collectors.toMap(
                        Doctor::getId, // el id como key del map
                        d -> d.getFirstName() + " " + d.getLastName()
                ));
    }

    private List<AppointmentInfo> toAppointmentInfoList(
            List<Appointment> appointments,
            Map<Long, String> doctorNames,
            String specialty
    ){
        return appointments.stream()
                .map(a -> new AppointmentInfo(
                        doctorNames.get(a.getDoctorId()),
                        specialty,
                        a.getDate().toString(),
                        a.getStartTime().toString()
                )).toList();
    }



}


















