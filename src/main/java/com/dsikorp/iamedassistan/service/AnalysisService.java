package com.dsikorp.iamedassistan.service;



import com.dsikorp.iamedassistan.dto.analysis.ConditionSummaryDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.SymptomAnalysisDto;

import java.util.List;

public interface AnalysisService {
    ConditionSummaryDto summarizeCondition(String condition, String model);
    List<ConditionSummaryDto> listRelatedConditions(String symptoms, String model);
    SymptomAnalysisDto analyzeSymptoms(String symptoms, String model);
    QueryClassificationDto classifyQuery(String query, String model);
}
