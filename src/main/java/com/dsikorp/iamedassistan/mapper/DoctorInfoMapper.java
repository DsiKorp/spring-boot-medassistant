package com.dsikorp.iamedassistan.mapper;

import com.dsikorp.iamedassistan.dto.DoctorInfo;
import com.dsikorp.iamedassistan.model.Doctor;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedSourcePolicy = ReportingPolicy.IGNORE)
public interface DoctorInfoMapper {

    DoctorInfo toDoctorInfo(Doctor doctor);
}
