package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssistantServiceImpl implements AssistantService {
    //    private final ChatClient geminiClient;
    //    private final ChatClient ollamaClient;
    private final ClientResolver clientResolver;

    @Value("classpath:prompts/explain-condition.st")
    private Resource explainConditionPrompt;

    @Value("classpath:prompts/symptom-analysis.st")
    private Resource symptomAnalysisPrompt;

    @Value("classpath:prompts/diagnosis-cot.st")
    private Resource diagnosisCotResource;

    @Value("classpath:prompts/consultation.st")
    private Resource consultationResource;

    private PromptTemplate explainConditionTemplate;

    private PromptTemplate symptomAnalysisTemplate;

    private PromptTemplate diagnosisCotTemplate;

    private PromptTemplate consultationTemplate;

//    public AssistantServiceImpl(
//            @Qualifier("geminiClient") ChatClient geminiClient,
//            @Qualifier("ollamaClient") ChatClient ollamaClient
//
//    ) {
//        this.geminiClient = geminiClient;
//        this.ollamaClient = ollamaClient;
//    }

    @PostConstruct
    void init() {
        consultationTemplate = new PromptTemplate(consultationResource);
        diagnosisCotTemplate = new PromptTemplate(diagnosisCotResource);
        symptomAnalysisTemplate = new PromptTemplate(symptomAnalysisPrompt);
        explainConditionTemplate = new PromptTemplate(explainConditionPrompt);
    }

    @Override
    public String chat(String prompt, String model) {
        log.info("Chat request - modelo: {} ", model);

        return clientResolver.resolve(model)
                .prompt(prompt).call().content();
    }

    @Override
    public Flux<String> chatStream(String prompt, String model) {
        log.info("Stream request - modelo: {} ", model);

        return clientResolver.resolve(model)
                .prompt(prompt).stream().content();
    }

    @Override
    public String explainCondition(String condition, String model) {
        log.info("Explain request — condición: {}, modelo: {}", condition, model);

//        return resolveClient(model)
//                .prompt()
//                .user(u -> u.text("Explicame qué es {condicion} en términos simbles para un paciente."
//                                + "Usa lenguaje accesible y evita jerga médica innecesaria.")
//                        .param("condicion", condition))
//                .call()
//                .content();

        String message = explainConditionTemplate.render(Map.of("condicion", condition));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String analyzeSymptoms(String symptoms, String model) {
        log.info("Análisis de síntomas: {}, modelo: {}", symptoms, model);

        String message = symptomAnalysisTemplate.render(Map.of("sintomas", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String diagnoseWithReasoning(String symptoms, String model) {
        log.info("Diagnóstico CoT — modelo: {}", model);

        String message = diagnosisCotTemplate.render(Map.of("sintomas", symptoms));

        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

    @Override
    public String consult(String query, String model) {
        log.info("Consulta médica — modelo: {}", model);
        String message = consultationTemplate.render(Map.of("consulta", query));
        return clientResolver.resolve(model)
                .prompt(message)
                .call()
                .content();
    }

//    private ChatClient resolveClient(String model) {
//        return "ollama".equalsIgnoreCase(model) ? ollamaClient : geminiClient;
//    }
}


