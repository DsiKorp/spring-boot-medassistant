package com.dsikorp.iamedassistan.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests unitarios de las restricciones de bean validation de {@link ChatRequestDto}.
 * Se usa el validador real de Hibernate Validator, sin levantar contexto de Spring.
 */
@DisplayName("ChatRequestDto - validación")
class ChatRequestDtoValidationTest {

    private ValidatorFactory factory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterEach
    void tearDown() {
        factory.close();
    }

    @Test
    @DisplayName("un prompt con solo espacios genera una única violación con el mensaje esperado")
    void blankPromptProducesExactlyOneViolation() {

        Set<ConstraintViolation<ChatRequestDto>> violaciones =
                validator.validate(new ChatRequestDto("   ", "gemini"));

        assertThat(violaciones).hasSize(1);

        ConstraintViolation<ChatRequestDto> violacion = violaciones.iterator().next();
        assertThat(violacion.getMessage()).isEqualTo("El prompt no puede estar vacío");
        assertThat(violacion.getPropertyPath().toString()).isEqualTo("prompt");
    }

    @Test
    @DisplayName("un prompt nulo genera una única violación")
    void nullPromptProducesExactlyOneViolation() {

        Set<ConstraintViolation<ChatRequestDto>> violaciones =
                validator.validate(new ChatRequestDto(null, "gemini"));

        assertThat(violaciones).hasSize(1);

        ConstraintViolation<ChatRequestDto> violacion = violaciones.iterator().next();
        assertThat(violacion.getMessage()).isEqualTo("El prompt no puede estar vacío");
        assertThat(violacion.getPropertyPath().toString()).isEqualTo("prompt");
    }

    @Test
    @DisplayName("un prompt válido no genera violaciones")
    void validPromptProducesNoViolations() {

        Set<ConstraintViolation<ChatRequestDto>> violaciones =
                validator.validate(new ChatRequestDto("¿Qué es la hipertensión?", "gemini"));

        assertThat(violaciones).isEmpty();
    }

    @Test
    @DisplayName("un model nulo no genera violaciones porque el campo no tiene restricciones")
    void nullModelProducesNoViolations() {

        Set<ConstraintViolation<ChatRequestDto>> violaciones =
                validator.validate(new ChatRequestDto("¿Qué es la hipertensión?", null));

        assertThat(violaciones).isEmpty();
    }
}
