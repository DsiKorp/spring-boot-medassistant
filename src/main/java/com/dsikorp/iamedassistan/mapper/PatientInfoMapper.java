package com.dsikorp.iamedassistan.mapper;

import com.dsikorp.iamedassistan.dto.PatientInfo;
import com.dsikorp.iamedassistan.model.Patient;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper entre PatientInfo DTO y Patient entity.
 */
@Mapper(componentModel = "spring")
public interface PatientInfoMapper {

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "allergies", target = "allergies")
    @Mapping(source = "conditions", target = "conditions")
    PatientInfo toPatientInfo(Patient patient);

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "allergies", target = "allergies")
    @Mapping(source = "conditions", target = "conditions")
    Patient toPatient(PatientInfo patientInfo);

}
