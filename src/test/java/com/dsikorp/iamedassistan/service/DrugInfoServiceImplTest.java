package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.DrugInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests unitarios de {@link DrugInfoServiceImpl}.
 * Se usa {@link MockRestServiceServer} sobre un {@link RestClient.Builder} para
 * simular la API de OpenFDA sin tocar la red.
 */
@DisplayName("DrugInfoServiceImpl")
class DrugInfoServiceImplTest {

    private static final String BASE_URL = "https://api.fda.gov";
    private static final String DRUG_NAME = "aspirin";
    private static final int MAX_FIELD_LENGTH = 500;

    private MockRestServiceServer server;
    private DrugInfoServiceImpl drugInfoService;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        server = MockRestServiceServer.bindTo(builder).build();
        drugInfoService = new DrugInfoServiceImpl(builder.build());
    }

    private void expectOpenFdaResponse(String json) {
        server.expect(requestTo(startsWith(BASE_URL + "/drug/label.json")))
                .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("getDrugInfo mapea el payload de OpenFDA a los campos del DrugInfo")
    void getDrugInfoMapsTheOpenFdaPayload() {

        String json = """
                {
                  "results": [
                    {
                      "openfda": {
                        "brand_name": ["TYLENOL"],
                        "generic_name": ["ACETAMINOPHEN"]
                      },
                      "purpose": ["Pain reliever and fever reducer"],
                      "warnings": ["Liver warning: this product contains acetaminophen"],
                      "dosage_and_administration": ["Adults: take 2 tablets every 6 hours"]
                    }
                  ]
                }
                """;

        expectOpenFdaResponse(json);

        DrugInfo info = drugInfoService.getDrugInfo(DRUG_NAME);

        assertThat(info).isNotNull();
        assertThat(info.brandName()).isEqualTo("TYLENOL");
        assertThat(info.genericName()).isEqualTo("ACETAMINOPHEN");
        assertThat(info.purpose()).isEqualTo("Pain reliever and fever reducer");
        assertThat(info.warnings()).isEqualTo("Liver warning: this product contains acetaminophen");
        assertThat(info.dosageAndAdministration()).isEqualTo("Adults: take 2 tablets every 6 hours");

        server.verify();
    }

    @Test
    @DisplayName("getDrugInfo devuelve 'No disponible' para los campos ausentes del payload")
    void getDrugInfoReturnsNoDisponibleForMissingFields() {

        String json = """
                {
                  "results": [
                    {
                      "purpose": ["Solo purpose"]
                    }
                  ]
                }
                """;

        expectOpenFdaResponse(json);

        DrugInfo info = drugInfoService.getDrugInfo(DRUG_NAME);

        assertThat(info).isNotNull();
        assertThat(info.brandName()).isEqualTo("No disponible");
        assertThat(info.genericName()).isEqualTo("No disponible");
        assertThat(info.purpose()).isEqualTo("Solo purpose");
        assertThat(info.warnings()).isEqualTo("No disponible");
        assertThat(info.dosageAndAdministration()).isEqualTo("No disponible");

        server.verify();
    }

    @Test
    @DisplayName("getDrugInfo devuelve null cuando la respuesta no trae la clave 'results'")
    void getDrugInfoReturnsNullWhenTheResponseHasNoResults() {

        String json = """
                {
                  "meta": {
                    "results": {
                      "total": 0
                    }
                  }
                }
                """;

        expectOpenFdaResponse(json);

        DrugInfo info = drugInfoService.getDrugInfo(DRUG_NAME);

        assertThat(info).isNull();

        server.verify();
    }

    @Test
    @DisplayName("getDrugInfo trunca a 500 caracteres los campos más largos que el máximo permitido")
    void getDrugInfoTruncatesFieldsLongerThanTheMaximumLength() {

        String textoLargo = "a".repeat(MAX_FIELD_LENGTH + 100);

        String json = """
                {
                  "results": [
                    {
                      "warnings": ["%s"]
                    }
                  ]
                }
                """.formatted(textoLargo);

        expectOpenFdaResponse(json);

        DrugInfo info = drugInfoService.getDrugInfo(DRUG_NAME);

        assertThat(info).isNotNull();
        assertThat(info.warnings())
                .isEqualTo("a".repeat(MAX_FIELD_LENGTH) + "...")
                .hasSize(MAX_FIELD_LENGTH + 3);

        server.verify();
    }
}
