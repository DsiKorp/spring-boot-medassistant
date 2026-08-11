package com.dsikorp.iamedassistan.service;



import com.dsikorp.iamedassistan.dto.DoctorInfo;

import java.util.List;

public interface DoctorService {
    List<DoctorInfo> searchDoctors(String query);
}
