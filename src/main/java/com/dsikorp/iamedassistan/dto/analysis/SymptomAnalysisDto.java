package com.dsikorp.iamedassistan.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

public record SymptomAnalysisDto(

        @JsonPropertyDescription("Lista de síntomas identificados en la consulta del paciente")
        List<String> identifiedSymptoms,

        @JsonPropertyDescription("Posibles condiciones médicas basadas en los síntomas, ordenadas de más a menos probable")
        List<PossibleConditionDto> possibleConditions,

        @JsonPropertyDescription("Nivel de urgencia general de la situación del paciente")
        UrgencyEnum urgencyLevel,

        @JsonPropertyDescription("Recomendación general para el paciente, incluyendo si debe buscar atención médica")
        String recommendation
) {


}
