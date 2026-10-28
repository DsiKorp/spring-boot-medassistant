package com.dsikorp.iamedassistan.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests unitarios puros de {@link ClientResolverAws}: en el perfil aws solo hay un proveedor (gemini).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientResolverAws")
class ClientResolverAwsTest {

    @Mock
    private ChatClient geminiClient;
    @Mock
    private ChatModel geminiModel;
    @Mock
    private VectorStore googleVectorStore;

    private ClientResolverAws resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientResolverAws(geminiClient, geminiModel, googleVectorStore);
    }

    @Test
    @DisplayName("resolve(null) devuelve el cliente de gemini")
    void resolveNullReturnsGeminiClient() {
        assertThat(resolver.resolve(null)).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolve acepta gemini ignorando mayúsculas y espacios")
    void resolveAcceptsGeminiVariants() {
        assertThat(resolver.resolve("gemini")).isSameAs(geminiClient);
        assertThat(resolver.resolve("GEMINI")).isSameAs(geminiClient);
        assertThat(resolver.resolve(" gemini ")).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolve rechaza cualquier proveedor distinto de gemini")
    void resolveRejectsOtherProviders() {
        assertThatThrownBy(() -> resolver.resolve("gpt-4o"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado en producción")
                .hasMessageContaining("gpt-4o");
    }

    @Test
    @DisplayName("resolveModel(null) devuelve el modelo de gemini")
    void resolveModelNullReturnsGeminiModel() {
        assertThat(resolver.resolveModel(null)).isSameAs(geminiModel);
    }

    @Test
    @DisplayName("resolveModel devuelve el modelo de gemini")
    void resolveModelReturnsGeminiModel() {
        assertThat(resolver.resolveModel("gemini")).isSameAs(geminiModel);
    }

    @Test
    @DisplayName("resolveModel rechaza proveedores no soportados en aws")
    void resolveModelRejectsOtherProviders() {
        assertThatThrownBy(() -> resolver.resolveModel("gpt-4o"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado en producción");
    }

    @Test
    @DisplayName("resolveVectorStore(null) devuelve el vector store de google")
    void resolveVectorStoreNullReturnsGoogleVectorStore() {
        assertThat(resolver.resolveVectorStore(null)).isSameAs(googleVectorStore);
    }

    @Test
    @DisplayName("resolveVectorStore devuelve el vector store de google")
    void resolveVectorStoreReturnsGoogleVectorStore() {
        assertThat(resolver.resolveVectorStore("gemini")).isSameAs(googleVectorStore);
    }

    @Test
    @DisplayName("resolveVectorStore rechaza proveedores no soportados en aws")
    void resolveVectorStoreRejectsOtherProviders() {
        assertThatThrownBy(() -> resolver.resolveVectorStore("gpt-4o"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado en producción");
    }
}
