package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.config.ClientResolver;
import com.dsikorp.iamedassistan.dto.analysis.ConditionSummaryDto;
import com.dsikorp.iamedassistan.dto.analysis.QueryClassificationDto;
import com.dsikorp.iamedassistan.dto.analysis.SymptomAnalysisDto;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService{

    private final ClientResolver clientResolver;

    @Value("classpath:prompts/structured-analysis.st")
    private Resource structuredAnalysisResource;

    private PromptTemplate structuredAnalysisTemplate;

    @PostConstruct
    void init(){
        structuredAnalysisTemplate = new PromptTemplate(structuredAnalysisResource);
    }

    @Override
    public ConditionSummaryDto summarizeCondition(String condition, String model) {

        log.info("1. Análisis estructurado de condición: {}, modelo: {}", condition, model);
        log.info("==================================================================================");

//        1 cadena, .entity() hace todo
        return clientResolver.resolve(model)
                .prompt()
                .user("Proporcioná un resumen médico educativo sobre: " + condition)
                .call()
                .entity(ConditionSummaryDto.class);

        // 5 pasos, enfoque manual
        ////BeanOutputConverter<ConditionSummary> converter = new BeanOutputConverter<>(ConditionSummary.class);
//        var converter = new BeanOutputConverter<>(ConditionSummaryDto.class);
//
//        String format = converter.getFormat();
//        log.info("2. Instrucciones de formato generadas converter.getFormat():\n{}", format);
//        log.info("==================================================================================");
//
//        String prompt = """
//                Proporcioná un resumen médico educativo sobre: %s
//
//                %s
//                """.formatted(condition, format);
//
//        log.info("3. prompt: {}", prompt);
//        log.info("==================================================================================");
//
//        String jsonResponse = clientResolver.resolve(model)
//                .prompt(prompt)
//                .call()
//                .content();
//        log.info("4. Respuesta JSON jsonResponse del modelo:\n{}", jsonResponse);
//        log.info("==================================================================================");
//
//        return converter.convert(Objects.requireNonNull(jsonResponse));
    }

    @Override
    public List<ConditionSummaryDto> listRelatedConditions(String symptoms, String model) {
        log.info("Listado de condiciones realacionadas - modelo: {} ", model);
        return clientResolver.resolve(model)
                .prompt()
                .user("Listá las 3 condiciones médicas más probables " +
                        "para estos síntomas: " + symptoms
                        )
                .call()
                .entity(new ParameterizedTypeReference<>() {
                });
    }

    @Override
    public SymptomAnalysisDto analyzeSymptoms(String symptoms, String model) {
        log.info("Análisis estructurado de síntomas — modelo: {}", model);

        String message = structuredAnalysisTemplate.render(
                Map.of("sintomas", symptoms)
        );

        return clientResolver.resolve(model)
                .prompt()
                .user(message)
                .call()
                .entity(SymptomAnalysisDto.class);
    }

    @Override
    public QueryClassificationDto classifyQuery(String query, String model) {
        log.info("Clasificación de consulta — modelo: {}", model);

        return clientResolver.resolve(model)
                .prompt()
                .user("Clasificá la siguiente consulta de un paciente. " +
                        "Determiná qué tipo de consulta es y explicá brevemente por qué.\n\n" +
                        "Consulta del paciente: \"" + query + "\"")
                .call()
                .entity(QueryClassificationDto.class);
    }
}
















