package com.dsikorp.iamedassistan.dto.analysis;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record QueryClassificationDto(
        @JsonPropertyDescription("Tipo de consulta identificada")
        QueryTypeEnum type,

        @JsonPropertyDescription("Explicación breve de por qué se clasificó de esta manera")
        String reason
) {
}
