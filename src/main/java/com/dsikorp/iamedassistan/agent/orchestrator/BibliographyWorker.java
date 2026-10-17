package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.config.ClientResolver;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class BibliographyWorker {

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/worker-bibliography.st")
    private Resource bibliographyResource;

    private PromptTemplate bibliographyTemplate;

    @PostConstruct
    void init() {
        bibliographyTemplate = new PromptTemplate(bibliographyResource);
    }

    private String interpret(List<Document> documents, String symptoms, String model){
        String documentContent = documents.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        String prompt = bibliographyTemplate.render(Map.of(
                "documentos", documentContent,
                "sintomas", symptoms
        ));

        String result = ChatClient.create(clientResolver.resolveModel(model))
                .prompt()
                .system("Sos un especialista en bibliografía médica.")
                .user(prompt)
                .call()
                .content();

        log.info("Worker bibliografía — interpretación completada");
        return result;
    }


    private List<Document> searchDocuments(String symptoms, String model){
        List<Document> documents = clientResolver.resolveVectorStore(model)
                .similaritySearch(
                        SearchRequest.builder()
                                .query(symptoms)
                                .topK(3)
                                .similarityThreshold(0.3)
                                .build());

        log.info("Worker bibliografía — documentos encontrados: {}", documents.size());
        return documents;
    }

    public String research(String symptoms, String model){
        List<Document> documents = searchDocuments(symptoms, model);

        if(documents.isEmpty()){
            return "No se encontraron documentos médicos relevantes para estos síntomas.";
        }

        return interpret(documents, symptoms, model);
    }


}














