package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.dto.ChatRequestDto;
import com.dsikorp.iamedassistan.dto.analysis.ConditionSummaryDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.SymptomAnalysisDto;
import com.dsikorp.iamedassistan.service.AnalysisService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/condition")
    public ResponseEntity<ConditionSummaryDto> analyzeCondition(@Valid @RequestBody ChatRequestDto request){
        return ResponseEntity.ok(analysisService.summarizeCondition(request.prompt(), request.model()));
    }

    @PostMapping("/conditions")
    public ResponseEntity<List<ConditionSummaryDto>> listConditions(@Valid @RequestBody ChatRequestDto request){
        return ResponseEntity.ok(analysisService.listRelatedConditions(request.prompt(), request.model()));
    }

    @PostMapping("/symptoms")
    public ResponseEntity<SymptomAnalysisDto> analyzeSymptoms(
            @Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(
                analysisService.analyzeSymptoms(
                        request.prompt(), request.model()));
    }

    @PostMapping("/classify")
    public ResponseEntity<QueryClassificationDto> classifyQuery(
            @Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(
                analysisService.classifyQuery(
                        request.prompt(), request.model()));
    }

}










