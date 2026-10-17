package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class ClinicalWorker {
    private final ClientResolver clientResolver;

    @Value("classpath:prompts/worker-clinical.st")
    private Resource clinicalResource;

    private PromptTemplate clinicalTemplate;

    @PostConstruct
    void init() {
        clinicalTemplate = new PromptTemplate(clinicalResource);
    }

    public String analyze(String symptoms, String model){
        String prompt = clinicalTemplate.render(Map.of("sintomas", symptoms));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un especialista en razonamiento clínico.")
                .user(prompt)
                .call()
                .content();

        log.info("Worker clínico — análisis completado {} ", result);

        return result;
    }
}
