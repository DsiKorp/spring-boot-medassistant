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
import org.springframework.core.env.Environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios puros de {@link ClientResolverDefault}: verifican el ruteo por proveedor
 * sin levantar contexto de Spring ni depender de red.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ClientResolverDefault")
class ClientResolverDefaultTest {

    @Mock
    private ChatClient geminiClient;
    @Mock
    private ChatClient ollamaClient;
    @Mock
    private ChatClient openaiClient;
    @Mock
    private ChatClient anthropicClient;
    @Mock
    private ChatClient groqClient;
    @Mock
    private ChatClient minimaxClient;

    @Mock
    private ChatModel geminiModel;
    @Mock
    private ChatModel ollamaModel;
    @Mock
    private ChatModel openaiModel;
    @Mock
    private ChatModel anthropicModel;

    @Mock
    private VectorStore googleVectorStore;
    @Mock
    private VectorStore ollamaVectorStore;
    @Mock
    private VectorStore openaiVectorStore;
    @Mock
    private VectorStore anthropicVectorStore;
    @Mock
    private VectorStore groqVectorStore;
    @Mock
    private VectorStore minimaxVectorStore;

    @Mock
    private Environment environment;

    private ClientResolverDefault resolver;

    @BeforeEach
    void setUp() {
        resolver = new ClientResolverDefault(
                geminiClient, ollamaClient, openaiClient, anthropicClient, groqClient, minimaxClient,
                geminiModel, ollamaModel, openaiModel, anthropicModel,
                googleVectorStore, ollamaVectorStore, openaiVectorStore,
                anthropicVectorStore, groqVectorStore, minimaxVectorStore,
                environment);
    }

    @Test
    @DisplayName("resolve(null) devuelve el cliente de gemini por defecto")
    void resolveNullReturnsGeminiClient() {
        assertThat(resolver.resolve(null)).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolve ignora mayúsculas y espacios en el nombre del proveedor")
    void resolveNormalizesProviderName() {
        assertThat(resolver.resolve("gemini")).isSameAs(geminiClient);
        assertThat(resolver.resolve("GEMINI")).isSameAs(geminiClient);
        assertThat(resolver.resolve(" gemini ")).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolve devuelve el cliente de cada proveedor soportado")
    void resolveReturnsProviderSpecificClient() {
        assertThat(resolver.resolve("ollama")).isSameAs(ollamaClient);
        assertThat(resolver.resolve("openai")).isSameAs(openaiClient);
        assertThat(resolver.resolve("anthropic")).isSameAs(anthropicClient);
        assertThat(resolver.resolve("groq")).isSameAs(groqClient);
        assertThat(resolver.resolve("minimax")).isSameAs(minimaxClient);
    }

    @Test
    @DisplayName("resolve falla con IllegalArgumentException para un proveedor desconocido")
    void resolveThrowsForUnknownProvider() {
        assertThatThrownBy(() -> resolver.resolve("desconocido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado")
                .hasMessageContaining("desconocido");
    }

    @Test
    @DisplayName("resolve funciona aunque el entorno no tenga el modelo configurado")
    void resolveToleratesMissingEnvironmentProperty() {
        when(environment.getProperty("GOOGLE_AI_MODEL")).thenReturn(null);

        assertThat(resolver.resolve("gemini")).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolve funciona cuando el entorno define el modelo especifico")
    void resolveToleratesConfiguredEnvironmentProperty() {
        when(environment.getProperty("GOOGLE_AI_MODEL")).thenReturn("gemini-2.5-flash");

        assertThat(resolver.resolve("gemini")).isSameAs(geminiClient);
    }

    @Test
    @DisplayName("resolveModel(null) devuelve el modelo de gemini")
    void resolveModelNullReturnsGeminiModel() {
        assertThat(resolver.resolveModel(null)).isSameAs(geminiModel);
    }

    @Test
    @DisplayName("resolveModel devuelve el modelo de cada proveedor soportado")
    void resolveModelReturnsProviderSpecificModel() {
        assertThat(resolver.resolveModel("gemini")).isSameAs(geminiModel);
        assertThat(resolver.resolveModel("ollama")).isSameAs(ollamaModel);
        assertThat(resolver.resolveModel("openai")).isSameAs(openaiModel);
        assertThat(resolver.resolveModel("anthropic")).isSameAs(anthropicModel);
    }

    @Test
    @DisplayName("resolveModel no soporta groq ni minimax")
    void resolveModelRejectsGroqAndMinimax() {
        assertThatThrownBy(() -> resolver.resolveModel("groq"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado");
        assertThatThrownBy(() -> resolver.resolveModel("minimax"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado");
    }

    @Test
    @DisplayName("resolveVectorStore(null) devuelve el vector store de google")
    void resolveVectorStoreNullReturnsGoogleVectorStore() {
        assertThat(resolver.resolveVectorStore(null)).isSameAs(googleVectorStore);
    }

    @Test
    @DisplayName("resolveVectorStore devuelve el vector store de cada proveedor soportado")
    void resolveVectorStoreReturnsProviderSpecificVectorStore() {
        assertThat(resolver.resolveVectorStore("gemini")).isSameAs(googleVectorStore);
        assertThat(resolver.resolveVectorStore("ollama")).isSameAs(ollamaVectorStore);
        assertThat(resolver.resolveVectorStore("openai")).isSameAs(openaiVectorStore);
        assertThat(resolver.resolveVectorStore("anthropic")).isSameAs(anthropicVectorStore);
        assertThat(resolver.resolveVectorStore("groq")).isSameAs(groqVectorStore);
        assertThat(resolver.resolveVectorStore("minimax")).isSameAs(minimaxVectorStore);
    }

    @Test
    @DisplayName("resolveVectorStore falla con IllegalArgumentException para un proveedor desconocido")
    void resolveVectorStoreThrowsForUnknownProvider() {
        assertThatThrownBy(() -> resolver.resolveVectorStore("desconocido"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no soportado")
                .hasMessageContaining("desconocido");
    }
}
