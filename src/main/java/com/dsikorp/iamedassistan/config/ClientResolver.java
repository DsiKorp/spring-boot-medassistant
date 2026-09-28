package com.dsikorp.iamedassistan.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ClientResolver {

    private final ChatClient geminiClient;
    private final ChatClient ollamaClient;
    private final ChatClient openaiClient;
    private final ChatClient anthropicClient;
    private final ChatClient groqClient;
    private final ChatClient minimaxClient;
    private final Environment environment;

    public ClientResolver(
            @Qualifier("geminiClient") ChatClient geminiClient,
            @Qualifier("ollamaClient") ChatClient ollamaClient,
            @Qualifier("openiaClient") ChatClient openaiClient,
            @Qualifier("anthropicClient") ChatClient anthropicClient,
            @Qualifier("groqClient") ChatClient groqClient,
            @Qualifier("minimaxClient") ChatClient minimaxClient,
            Environment environment) {
        this.geminiClient = geminiClient;
        this.ollamaClient = ollamaClient;
        this.openaiClient = openaiClient;
        this.anthropicClient = anthropicClient;
        this.groqClient = groqClient;
        this.minimaxClient = minimaxClient;
        this.environment = environment;
    }

    public ChatClient resolve(String model){
        log.info("resolveClient Proveedor modelo: {}", model);
        log.info("resolveClient modelo específico: {}", modelEnvSearch(model));

        if ("ollama".equalsIgnoreCase(model)) {

            return ollamaClient;
        }
        if ("openai".equalsIgnoreCase(model)) {
            return openaiClient;
        }
        if ("anthropic".equalsIgnoreCase(model)) {
            return anthropicClient;
        }
        if ("groq".equalsIgnoreCase(model)) {
            return groqClient;
        }
        if ("minimax".equalsIgnoreCase(model)) {
            return minimaxClient;
        }

        return geminiClient;
    }

    /**
     * Dado el nombre del proveedor (gemini, ollama, openai, anthropic, groq, minimax)
     * devuelve el nombre específico del modelo configurado en el archivo .env.
     * Si el proveedor es desconocido o la variable no existe, devuelve "".
     *
     * Las variables se leen del {@link Environment} de Spring, que ya tiene cargadas
     * las entradas del .env gracias a {@code DotenvEnvironmentPostProcessor}.
     */
    private String modelEnvSearch(String model) {
        if (model == null) {
            return "";
        }
        String envKey = switch (model.toLowerCase()) {
            case "ollama"    -> "OLLAMA_AI_MODEL";
            case "openai"    -> "OPENAI_AI_MODEL";
            case "anthropic" -> "ANTHROPIC_AI_MODEL";
            case "groq"      -> "GROQ_AI_MODEL";
            case "minimax"   -> "MINIMAX_AI_MODEL";
            case "gemini"    -> "GOOGLE_AI_MODEL";
            default          -> null;
        };
        if (envKey == null) {
            log.warn("Proveedor '{}' sin variable de modelo asociada en .env", model);
            return "";
        }
        String value = environment.getProperty(envKey);
        if (value == null || value.isBlank()) {
            log.warn("Variable '{}' no encontrada en .env o vacía", envKey);
            return "";
        }
        return value;
    }
}