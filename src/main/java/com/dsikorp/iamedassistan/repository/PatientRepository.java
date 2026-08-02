package com.dsikorp.iamedassistan.repository;

import com.dsikorp.iamedassistan.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PatientRepository extends JpaRepository<Patient, Long> {
}
