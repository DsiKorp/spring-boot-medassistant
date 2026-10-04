package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.dto.ChatRequestDto;
import com.dsikorp.iamedassistan.service.AssistantService;
import com.dsikorp.iamedassistan.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final AssistantService assistantService;
    private final SecurityUtils securityUtils;

    @PostMapping
    public ResponseEntity<String> chat(@Valid @RequestBody ChatRequestDto request,
                                       @AuthenticationPrincipal Jwt jwt,
                                       Authentication authentication
                                       //@RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
    ) {
        Long userId = jwt.getClaim("userId");
        String role = securityUtils.extractRole(authentication);
        log.info("userId: {}", userId);
        log.info("role: {}", role);

        return ResponseEntity.ok(assistantService.chat(request.prompt(), request.model(), userId, role));
    }

    @PostMapping(value = "/stream", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> chatStream(@Valid @RequestBody ChatRequestDto request,
                                   @AuthenticationPrincipal Jwt jwt,
                                   Authentication authentication)
    {
        Long userId = jwt.getClaim("userId");
        String role = securityUtils.extractRole(authentication);
        log.info("userId: {}", userId);
        log.info("role: {}", role);

        return assistantService.chatStream(request.prompt(), request.model(),  userId, role);
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
    public ResponseEntity<String> diagnoseWithReasoning(@Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(assistantService.diagnoseWithReasoning(
                        request.prompt(), request.model()));
    }

    @PostMapping("/consult")
    public ResponseEntity<String> consult(@Valid @RequestBody ChatRequestDto request) {
        return ResponseEntity.ok(assistantService.consult(
                        request.prompt(), request.model()));
    }
}