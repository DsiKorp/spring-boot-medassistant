package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.dto.ChatRequestDto;
import com.dsikorp.iamedassistan.service.AssistantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final AssistantService assistantService;

    @PostMapping
    public ResponseEntity<String> chat(
            @RequestBody ChatRequestDto request
    ) {
        return ResponseEntity.ok(assistantService.chat(request.prompt(), request.model()));
    }

    @PostMapping(value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> chatStream(
            @RequestBody ChatRequestDto request
    ) {
        return assistantService.chatStream(request.prompt(), request.model());
    }

    @PostMapping("/explain")
    public ResponseEntity<String> explainCondition(@Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(assistantService.explainCondition(request.prompt(), request.model()));
    }

    @PostMapping("/symptoms")
    public ResponseEntity<String> analyzeSymptoms(@Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(assistantService.analyzeSymptoms(request.prompt(), request.model()));
    }

    @PostMapping("/diagnose")
    public ResponseEntity<String> diagnoseWithReasoning(
            @Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(
                assistantService.diagnoseWithReasoning(
                        request.prompt(), request.model()));
    }

    @PostMapping("/consult")
    public ResponseEntity<String> consult(
            @Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(
                assistantService.consult(
                        request.prompt(), request.model()));
    }
}