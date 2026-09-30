package com.dsikorp.iamedassistan.mapper;

import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.model.Doctor;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * Mapper entre DoctorInfo DTO y Doctor entity.
 */
@Mapper(componentModel = "spring")
public interface DoctorInfoMapper {

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "specialty", target = "specialty")
    @Mapping(source = "licenseNumber", target = "licenseNumber")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "office", target = "office")
    Doctor toDoctor(DoctorInfo doctorInfo);

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    @Mapping(source = "specialty", target = "specialty")
    @Mapping(source = "licenseNumber", target = "licenseNumber")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "office", target = "office")
    DoctorInfo toDoctorInfo(Doctor doctor);

}
