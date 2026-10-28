package com.dsikorp.iamedassistan.agent.orchestrator;

import com.dsikorp.iamedassistan.config.ClientResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests del orquestador.
 *
 * <p>Acá se stubbea el {@link ChatModel} en lugar de interceptar el método estático
 * {@code ChatClient.create(...)}: la variante con progreso corre la síntesis en
 * {@code Schedulers.boundedElastic()}, y el mock estático de Mockito es thread-local,
 * así que no alcanzaría. Con un {@code ChatModel} stubbeado el {@code ChatClient} real
 * funciona en cualquier hilo.
 */
@ExtendWith(MockitoExtension.class)
class OrchestratorServiceImplTest {

    private static final String SINTOMAS = "fiebre, tos seca y dolor muscular";
    private static final Long USER_ID = 7L;

    @Mock
    private ClientResolver clientResolver;

    @Mock
    private ClinicalWorker clinicalWorker;

    @Mock
    private HistoryWorker historyWorker;

    @Mock
    private BibliographyWorker bibliographyWorker;

    @Mock
    private ChatModel chatModel;

    private OrchestratorServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrchestratorServiceImpl(clientResolver, clinicalWorker, historyWorker, bibliographyWorker);

        ReflectionTestUtils.setField(service, "synthesisResource", new ClassPathResource("prompts/orchestrator-synthesis.st"));

        service.init();

        when(clientResolver.resolveModel("gemini")).thenReturn(chatModel);
        when(chatModel.getOptions()).thenReturn(ChatOptions.builder().model("modelo-de-prueba").build());
        when(chatModel.call(any(Prompt.class))).thenReturn(
                ChatResponse.builder()
                        .generations(List.of(new Generation(new AssistantMessage("RESPUESTA FINAL AL PACIENTE"))))
                        .build());
    }

    @Test
    @DisplayName("el orquestador consulta a los tres workers y devuelve la síntesis")
    void orchestrateRunsAllWorkersAndReturnsSynthesis() {
        when(clinicalWorker.analyze(SINTOMAS, "gemini")).thenReturn("ANÁLISIS CLÍNICO");
        when(historyWorker.lookup(USER_ID)).thenReturn("HISTORIAL DEL PACIENTE");
        when(bibliographyWorker.research(SINTOMAS, "gemini")).thenReturn("BIBLIOGRAFÍA MÉDICA");

        String response = service.orchestrate(SINTOMAS, "gemini", USER_ID);

        assertThat(response).isEqualTo("RESPUESTA FINAL AL PACIENTE");
        verify(clinicalWorker).analyze(SINTOMAS, "gemini");
        verify(historyWorker).lookup(USER_ID);
        verify(bibliographyWorker).research(SINTOMAS, "gemini");
    }

    @Test
    @DisplayName("la síntesis integra los tres insumos del prompt de orquestación")
    void synthesisPromptIntegratesAllThreeWorkerOutputs() {
        when(clinicalWorker.analyze(SINTOMAS, "gemini")).thenReturn("ANÁLISIS CLÍNICO");
        when(historyWorker.lookup(USER_ID)).thenReturn("HISTORIAL DEL PACIENTE");
        when(bibliographyWorker.research(SINTOMAS, "gemini")).thenReturn("BIBLIOGRAFÍA MÉDICA");

        service.orchestrate(SINTOMAS, "gemini", USER_ID);

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        String rendered = promptCaptor.getValue().getContents();
        assertThat(rendered)
                .contains("ANÁLISIS CLÍNICO")
                .contains("HISTORIAL DEL PACIENTE")
                .contains("BIBLIOGRAFÍA MÉDICA")
                .contains(SINTOMAS);
    }

    @Test
    @DisplayName("la variante con progreso emite los marcadores de etapa y la respuesta final")
    void progressVariantEmitsStageMarkersAndFinalResponse() {
        when(clinicalWorker.analyze(SINTOMAS, "gemini")).thenReturn("ANÁLISIS CLÍNICO");
        when(historyWorker.lookup(USER_ID)).thenReturn("HISTORIAL DEL PACIENTE");
        when(bibliographyWorker.research(SINTOMAS, "gemini")).thenReturn("BIBLIOGRAFÍA MÉDICA");

        List<String> chunks = service.orchestrateWithProgress(SINTOMAS, "gemini", USER_ID)
                .collectList()
                .block(Duration.ofSeconds(10));

        assertThat(chunks).isNotNull();
        assertThat(chunks).containsExactly(
                "[PROGRESO] Analizando síntomas clínicamente...",
                "[PROGRESO] Consultando historial del paciente...",
                "[PROGRESO] Buscando en bibliografía médica...",
                "[PROGRESO] Integrando resultados de los tres especialistas...",
                "RESPUESTA FINAL AL PACIENTE"
        );
    }
}
