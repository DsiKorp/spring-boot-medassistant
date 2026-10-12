package com.dsikorp.iamedassistan.service;

import reactor.core.publisher.Flux;

public interface AssistantService {
    String chat(String prompt, String model, Long userId, String role);
    Flux<String> chatStream(String prompt, String model, Long userId, String role);
    String explainCondition(String condition, String model, Long userId);
    String analyzeSymptoms(String symptoms, String model, Long userId);
    String diagnoseWithReasoning(String symptoms, String model, Long userId);
    String consult(String query, String model, Long userId);
}
