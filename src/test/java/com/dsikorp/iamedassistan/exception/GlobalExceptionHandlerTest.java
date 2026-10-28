package com.dsikorp.iamedassistan.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;

import java.nio.file.AccessDeniedException;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de {@link GlobalExceptionHandler}.
 * Se invocan los handlers directamente, sin contexto de Spring.
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleRestClientException responde 503 con el mensaje de servicio externo no disponible")
    void handleRestClientExceptionReturnsServiceUnavailable() {

        ResponseEntity<String> respuesta =
                handler.handleRestClientException(new RestClientException("Fallo de red"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(503);
        assertThat(respuesta.getBody()).isEqualTo("El servicio externo no está disponible en este momento.");
    }

    @Test
    @DisplayName("handleDateTimeParseException responde 400 con la indicación del formato yyyy-MM-dd")
    void handleDateTimeParseExceptionReturnsBadRequest() {

        ResponseEntity<String> respuesta =
                handler.handleDateTimeParseException(new DateTimeParseException("texto", "yyyy-MM-dd", 0));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).isEqualTo("Formato de fecha inválido. Use el formato yyyy-MM-dd.");
    }

    @Test
    @DisplayName("handleIllegalArgumentException responde 400 con el mensaje original de la excepción")
    void handleIllegalArgumentExceptionReturnsBadRequestWithTheOriginalMessage() {

        ResponseEntity<String> respuesta =
                handler.handleIllegalArgumentException(new IllegalArgumentException("modelo desconocido"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(400);
        assertThat(respuesta.getBody()).isEqualTo("modelo desconocido");
    }

    @Test
    @DisplayName("handleAccessDeniedException responde 403 con el mensaje de permisos insuficientes")
    void handleAccessDeniedExceptionReturnsForbidden() {

        ResponseEntity<String> respuesta =
                handler.handleAccessDeniedException(new AccessDeniedException("/etc/secret"));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(403);
        assertThat(respuesta.getBody()).isEqualTo("No tenés permisos para realizar esta acción.");
    }
}
