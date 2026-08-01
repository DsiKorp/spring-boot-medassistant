package com.dsikorp.iamedassistan.dto;

import jakarta.validation.constraints.NotBlank;

public record ChatRequestDto(
        @NotBlank(message = "El prompt no puede estar vacío")
        String prompt,
        String model
) {
}
