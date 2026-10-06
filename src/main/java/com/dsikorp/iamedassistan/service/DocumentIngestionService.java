package com.dsikorp.iamedassistan.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocumentIngestionService {

    private final VectorStore googleVectorStore;
    private final VectorStore ollamaVectorStore;
    private final VectorStore openiaVectorStore;
    private final VectorStore anthropicVectorStore;
    private final VectorStore groqVectorStore;
    private final VectorStore minimaxVectorStore;

    @Value("classpath:docs/farmacologia-ibuprofeno.pdf")
    private Resource ibuprofenoDoc;

    @Value("classpath:docs/guia-hipertension-medassistant.pdf")
    private Resource hipertensionDoc;

    @Value("classpath:docs/protocolo-atencion-cardiovascular.pdf")
    private Resource cardiovascularDoc;

    public DocumentIngestionService(
            @Qualifier("googleVectorStore") VectorStore googleVectorStore,
            @Qualifier("ollamaVectorStore") VectorStore ollamaVectorStore,
            @Qualifier("openiaVectorStore") VectorStore openiaVectorStore,
            @Qualifier("anthropicVectorStore") VectorStore anthropicVectorStore,
            @Qualifier("groqVectorStore") VectorStore groqVectorStore,
            @Qualifier("minimaxVectorStore") VectorStore minimaxVectorStore
    ) {
        this.googleVectorStore = googleVectorStore;
        this.ollamaVectorStore = ollamaVectorStore;
        this.openiaVectorStore = openiaVectorStore;
        this.anthropicVectorStore = anthropicVectorStore;
        this.groqVectorStore = groqVectorStore;
        this.minimaxVectorStore = minimaxVectorStore;
    }

    @PostConstruct
    public void ingest(){
        log.info("Iniciando ingestión de documentos médicos...");
        List<Document> allDocuments = new ArrayList<>();
        allDocuments.addAll(readDocument(ibuprofenoDoc));
        allDocuments.addAll(readDocument(hipertensionDoc));
        allDocuments.addAll(readDocument(cardiovascularDoc));

        TokenTextSplitter splitter = TokenTextSplitter.builder()
                .withChunkSize(800)
                .withMaxNumChunks(1000)
                .build();

        List<Document> chunks = splitter.apply(allDocuments);

        log.info("Documentos divididos en {} fragmentos", chunks.size());

        log.info("Ingiriendo en collection Google...");
        googleVectorStore.add(chunks);

        log.info("Ingiriendo en collection Ollama...");
        ollamaVectorStore.add(chunks);

        log.info("Ingiriendo en collection OpenIa...");
        openiaVectorStore.add(chunks);

        log.info("Ingiriendo en collection Anthropic...");
        anthropicVectorStore.add(chunks);

        log.info("Ingiriendo en collection Groq...");
        groqVectorStore.add(chunks);

        log.info("Ingiriendo en collection MiniMax...");
        minimaxVectorStore.add(chunks);
    }


    private List<Document> readDocument(Resource resource){
        log.info("Leyendo documento: {}", resource.getFilename());
        TikaDocumentReader reader = new TikaDocumentReader(resource);
        return reader.get();
    }


}





