package com.dsikorp.iamedassistan.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios puros de {@link SymptomConsultationPromptProvider}: el prompt MCP
 * "symptom-consultation" arma un unico mensaje de usuario con la estructura educativa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SymptomConsultationPromptProvider")
class SymptomConsultationPromptProviderTest {

    private final SymptomConsultationPromptProvider provider = new SymptomConsultationPromptProvider();

    private static String textOf(McpSchema.GetPromptResult result) {
        assertThat(result.messages()).hasSize(1);
        McpSchema.PromptMessage message = result.messages().get(0);
        assertThat(message.content()).isInstanceOf(McpSchema.TextContent.class);
        return ((McpSchema.TextContent) message.content()).text();
    }

    @Test
    @DisplayName("Devuelve un unico mensaje de usuario")
    void returnsSingleUserMessage() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("dolor de cabeza");

        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).role()).isEqualTo(McpSchema.Role.USER);
    }

    @Test
    @DisplayName("El mensaje incluye el sintoma consultado")
    void messageContainsTheSymptom() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("dolor de cabeza");

        assertThat(textOf(result)).contains("dolor de cabeza");
    }

    @Test
    @DisplayName("El mensaje pide causas, consulta y especialidad")
    void messageContainsRequestedSections() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("mareos");

        String text = textOf(result);
        assertThat(text).contains("Posibles causas comunes");
        assertThat(text).contains("Cuándo consultar a un profesional");
        assertThat(text).contains("Especialidad médica recomendada");
    }

    @Test
    @DisplayName("El mensaje incluye la advertencia de uso educativo")
    void messageContainsEducationalDisclaimer() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("fiebre");

        assertThat(textOf(result)).contains("esta información es solo educativa");
        assertThat(textOf(result)).contains("no reemplaza");
    }

    @Test
    @DisplayName("El resultado describe la consulta sobre sintomas")
    void resultHasDescription() {
        McpSchema.GetPromptResult result = provider.symptomConsultation("tos");

        assertThat(result.description()).isEqualTo("Consulta sobre síntomas");
    }
}
