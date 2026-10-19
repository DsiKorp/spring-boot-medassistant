package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.agent.AppointmentChainService;
import com.dsikorp.iamedassistan.agent.OpenAgent.OpenAgentService;
import com.dsikorp.iamedassistan.agent.RoutingWorkflowService;
import com.dsikorp.iamedassistan.agent.orchestrator.OrchestratorService;
import com.dsikorp.iamedassistan.dto.ChatRequestDto;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AppointmentChainService appointmentChainService;
    private final RoutingWorkflowService routingWorkflowService;
    private final OrchestratorService orchestratorService;
    private final OpenAgentService openAgentService;

    @PostMapping("/chain/appointment")
    public ResponseEntity<String> bookAppointmentChain(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt
    ){

        Long userId = jwt.getClaim("userId");

        return ResponseEntity
                .ok(appointmentChainService
                        .bookAppointmentChain(request.prompt(), request.model(), userId));
    }

    @PostMapping("/routing")
    public ResponseEntity<String> routeQuery(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                routingWorkflowService.routeQuery(request.prompt(), request.model(), userId));
    }

    @PostMapping("/orchestrator")
    public ResponseEntity<String> orchestrate(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                orchestratorService.orchestrate(request.prompt(), request.model(), userId));
    }

    @PostMapping("/chain/appointment/confirm")
    public ResponseEntity<String> confirmBooking(
            @AuthenticationPrincipal Jwt jwt
    ){
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(appointmentChainService.confirmBooking(userId));
    }

    @PostMapping("/chain/appointment/cancel")
    public ResponseEntity<String> cancelBooking(@AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(appointmentChainService.cancelBooking(userId));
    }

    @PostMapping(value = "/orchestrator/progress", produces = "text/event-stream; charset=UTF-8")
    public Flux<String> orchestrateWithProgress(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return orchestratorService.orchestrateWithProgress(request.prompt(), request.model(), userId);
    }

    @PostMapping("/open")
    public ResponseEntity<String> openAgent(
            @Valid @RequestBody ChatRequestDto request,
            @AuthenticationPrincipal Jwt jwt) {
        Long userId = jwt.getClaim("userId");
        return ResponseEntity.ok(
                openAgentService.execute(request.prompt(), request.model(), userId));
    }
}
