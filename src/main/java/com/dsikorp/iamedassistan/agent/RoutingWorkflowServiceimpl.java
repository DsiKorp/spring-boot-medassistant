package com.dsikorp.iamedassistan.agent;

import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.config.MedicalAuditAdvisor;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.service.AnalysisService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoutingWorkflowServiceimpl implements RoutingWorkflowService {

    private final ClientResolver clientResolver;
    private final AnalysisService analysisService;
    private final MedicalAuditAdvisor medicalAuditAdvisor;

    @Value("classpath:prompts/routing-emergency.st")
    private Resource emergencyResource;

    @Value("classpath:prompts/routing-symptom.st")
    private Resource symptomResource;

    @Value("classpath:prompts/routing-general.st")
    private Resource generalResource;

    @Value("classpath:prompts/routing-prescription.st")
    private Resource prescriptionResource;

    private PromptTemplate emergencyTemplate;
    private PromptTemplate symptomTemplate;
    private PromptTemplate generalTemplate;
    private PromptTemplate prescriptionTemplate;

    @PostConstruct
    void init() {
        emergencyTemplate = new PromptTemplate(emergencyResource);
        symptomTemplate = new PromptTemplate(symptomResource);
        generalTemplate = new PromptTemplate(generalResource);
        prescriptionTemplate = new PromptTemplate(prescriptionResource);
    }

    @Override
    public String routeQuery(String query, String model, Long userId) {
        QueryClassificationDto classification = analysisService.classifyQuery(query, model, userId);

        log.info("Router — tipo: {} — razón: {}", classification.type(), classification.reason());

        return switch (classification.type()){
            case SYMPTOM_REPORT -> handleSymptomReport(query,model,userId);
            case GENERAL_QUESTION -> handleGeneralQuestion(query,model,userId);
            case EMERGENCY -> handleEmergency(query,model,userId);
            case PRESCRIPTION_REQUEST -> handlePrescriptionRequest(query,model,userId);
            case OFF_TOPIC -> handleOffTopic(userId);
        };
    }

    private String handleEmergency(String query, String model, Long userId){
        log.info("Ruta: EMERGENCY");
        String message = emergencyTemplate.render(Map.of("consulta", query));

        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un especialista en urgencias médicas. Respondé de forma breve y directa.")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId))
                )
                .call()
                .content();
    }

    private String handleSymptomReport(String query, String model, Long userId) {
        log.info("Ruta: SYMPTOM_REPORT");
        String message = symptomTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un especialista en análisis de síntomas.")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleGeneralQuestion(String query, String model, Long userId) {
        log.info("Ruta: GENERAL_QUESTION");
        String message = generalTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un médico educador. Respondé con detalle y claridad.")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handlePrescriptionRequest(String query, String model, Long userId) {
        log.info("Ruta: PRESCRIPTION_REQUEST");
        String message = prescriptionTemplate.render(Map.of("consulta", query));
        return ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un farmacólogo clínico. Informás sobre medicamentos sin prescribir.")
                .user(message)
                .advisors(a -> a.advisors(medicalAuditAdvisor)
                        .param(ChatMemory.CONVERSATION_ID, String.valueOf(userId)))
                .call()
                .content();
    }

    private String handleOffTopic(Long userId) {
        log.info("Ruta: OFF_TOPIC — userId: {} — respuesta determinística, sin llamada al modelo", userId);
        return "Lo siento, solo puedo ayudarte con consultas relacionadas con salud y medicina. " +
                "Si tenés una consulta médica, no dudes en escribirme.";
    }
}
