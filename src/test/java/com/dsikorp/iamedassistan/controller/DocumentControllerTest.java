package com.dsikorp.iamedassistan.controller;

import com.dsikorp.iamedassistan.MockMvcTestSupport;
import com.dsikorp.iamedassistan.dto.IngestionResult;
import com.dsikorp.iamedassistan.service.DocumentIngestionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class DocumentControllerTest {

    @Mock
    private DocumentIngestionService documentIngestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcTestSupport.standalone(new DocumentController(documentIngestionService)).build();
    }

    @AfterEach
    void tearDown() {
        MockMvcTestSupport.clearSecurityContext();
    }

    @Test
    @DisplayName("la ingesta devuelve el nombre del archivo y la cantidad de fragmentos")
    void ingestReturnsIngestionResult() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "protocolo.pdf", "application/pdf", "contenido".getBytes(StandardCharsets.UTF_8));

        when(documentIngestionService.ingestNewDocument(any(Resource.class)))
                .thenReturn(new IngestionResult("protocolo.pdf", 3));

        mockMvc.perform(multipart("/api/v1/documents/ingest")
                        .file(file)
                        .with(MockMvcTestSupport.asPatient(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filename").value("protocolo.pdf"))
                .andExpect(jsonPath("$.fragmentsCount").value(3));
    }

    @Test
    @DisplayName("el archivo subido se entrega al servicio de ingesta")
    void fileIsForwardedToIngestionService() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "guia.txt", "text/plain", "contenido".getBytes(StandardCharsets.UTF_8));

        when(documentIngestionService.ingestNewDocument(any(Resource.class)))
                .thenReturn(new IngestionResult("guia.txt", 1));

        mockMvc.perform(multipart("/api/v1/documents/ingest")
                        .file(file)
                        .with(MockMvcTestSupport.asPatient(1L)))
                .andExpect(status().isOk());

        ArgumentCaptor<Resource> resource = ArgumentCaptor.forClass(Resource.class);
        verify(documentIngestionService).ingestNewDocument(resource.capture());

        assertThat(resource.getValue().getFilename()).isEqualTo("guia.txt");
    }

    @Test
    @DisplayName("una carga sin contenido llega al servicio: el endpoint hoy no valida el tamaño")
    void emptyFileIsForwardedWithoutValidation() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "vacio.txt", "text/plain", new byte[0]);

        when(documentIngestionService.ingestNewDocument(any(Resource.class)))
                .thenReturn(new IngestionResult("vacio.txt", 0));

        mockMvc.perform(multipart("/api/v1/documents/ingest")
                        .file(file)
                        .with(MockMvcTestSupport.asPatient(1L)))
                .andExpect(status().isOk());

        verify(documentIngestionService).ingestNewDocument(any(Resource.class));
    }
}
