package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrchestratorServiceImpl implements OrchestratorService{

    private final ClientResolver clientResolver;
    private final ClinicalWorker clinicalWorker;
    private final HistoryWorker historyWorker;
    private final BibliographyWorker bibliographyWorker;

    @Value("classpath:prompts/orchestrator-synthesis.st")
    private Resource synthesisResource;

    private PromptTemplate synthesisTemplate;

    @PostConstruct
    void init() {
        synthesisTemplate = new PromptTemplate(synthesisResource);
    }

    @Override
    public String orchestrate(String symptoms, String model, Long userId) {

        log.info("Orquestador — iniciando análisis integral");

        String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);
        String patientHistory = historyWorker.lookup(userId);
        String bibliographyResearch = bibliographyWorker.research(symptoms, model);

        log.info("Orquestador — los tres workers completaron su trabajo");

        return synthesize(clinicalAnalysis,patientHistory,bibliographyResearch,symptoms,model);
    }

    private String synthesize(String clinicalAnalysis, String patientHistory,
                              String bibliographyResearch, String symptoms, String model){

        String prompt = synthesisTemplate.render(Map.of(
                "analisisClinico", clinicalAnalysis,
                "historial", patientHistory,
                "bibliografia", bibliographyResearch,
                "sintomas", symptoms
        ));

        //"Sos un médico que integra información de múltiples fuentes para dar una respuesta completa."

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un médico que integra información de múltiples fuentes para dar una respuesta completa.")
                .user(prompt)
                .call()
                .content();

        log.info("Orquestador — síntesis completada");
        return result;
    }


    @Override
    public Flux<String> orchestrateWithProgress(String symptoms, String model, Long userId) {

        return Flux.<String>create( sink -> {
            try {
                sink.next("[PROGRESO] Analizando síntomas clínicamente...");
                String clinicalAnalysis = clinicalWorker.analyze(symptoms, model);

                sink.next("[PROGRESO] Consultando historial del paciente...");
                String patientHistory = historyWorker.lookup(userId);

                sink.next("[PROGRESO] Buscando en bibliografía médica...");
                String bibliographyResearch = bibliographyWorker.research(symptoms, model);

                sink.next("[PROGRESO] Integrando resultados de los tres especialistas...");
                String result = synthesize(clinicalAnalysis,patientHistory,bibliographyResearch,symptoms,model);

                sink.next(result);
                sink.complete();
            } catch (Exception e) {
                log.error("Orquestador (progreso) — error: {}", e.getMessage());
                sink.error(e);
            }
            // deja libre el http para que se ejecute lo anterior
        }).subscribeOn(Schedulers.boundedElastic());
    }
}



















