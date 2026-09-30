package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.PatientInfo;

public interface PatientService {
    PatientInfo getPatientInfo(Long patientId);
}
