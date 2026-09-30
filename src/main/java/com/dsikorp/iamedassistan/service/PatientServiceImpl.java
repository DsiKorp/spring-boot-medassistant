package com.dsikorp.iamedassistan.service;


import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.mapper.PatientInfoMapper;
import com.dsikorp.iamedassistan.model.Patient;
import com.dsikorp.iamedassistan.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final PatientInfoMapper patientInfoMapper;

    @Transactional(readOnly = true)
    @Override
    public PatientInfo getPatientInfo(Long patientId) {

        log.info("Consultando historial: patientId={}", patientId);

        return patientRepository.findById(patientId)
                .map(patientInfoMapper::toPatientInfo)
                .orElse(null);
    }
}
