package com.dsikorp.iamedassistan.service;


import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.mapper.DoctorInfoMapper;
import com.dsikorp.iamedassistan.model.Doctor;
import com.dsikorp.iamedassistan.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorServiceImpl implements DoctorService{

    private final DoctorRepository doctorRepository;
    private final DoctorInfoMapper doctorInfoMapper;

    @Transactional(readOnly = true)
    @Override
    public List<DoctorInfo> searchDoctors(String query){

        return doctorRepository
                .findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecialtyContainingIgnoreCase(
                        query, query, query
                )
                .stream()
                .map(doctorInfoMapper::toDoctorInfo)
                .toList();

    }

//    private DoctorInfo toDoctorInfo(Doctor doctor) {
//        return new DoctorInfo(
//                doctor.getFirstName(),
//                doctor.getLastName(),
//                doctor.getSpecialty(),
//                doctor.getLicenseNumber(),
//                doctor.getPhone(),
//                doctor.getOffice());
//    }
}
