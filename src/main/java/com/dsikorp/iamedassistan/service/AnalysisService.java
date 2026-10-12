package com.dsikorp.iamedassistan.service;



import com.dsikorp.iamedassistan.dto.analysis.ConditionSummaryDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.SymptomAnalysisDto;

import java.util.List;

public interface AnalysisService {
    ConditionSummaryDto summarizeCondition(String condition, String model, Long useId);
    List<ConditionSummaryDto> listRelatedConditions(String symptoms, String model, Long useId);
    SymptomAnalysisDto analyzeSymptoms(String symptoms, String model, Long useId);
    QueryClassificationDto classifyQuery(String query, String model, Long useId);
}
