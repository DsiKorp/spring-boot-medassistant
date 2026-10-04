package com.dsikorp.iamedassistan.service;

import com.dsikorp.iamedassistan.dto.DrugInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class DrugInfoServiceImpl implements DrugInfoService {

    private final RestClient openFdaClient;
    private static final int MAX_FIELD_LENGTH = 500;

    @Override
    public DrugInfo getDrugInfo(String drugName) {

        log.info("Consultando OpenFDA: drugName={}", drugName);

        var response = fetchFromOpenFda(drugName);

        return mapToDrugInfo(response, drugName);
    }

    private String extractFirst(Map<?, ?> map, String key){

        if(!(map.get(key) instanceof List<?> list) || list.isEmpty()){
            return "No disponible";
        }

        var value = list.getFirst().toString();

        return value.length() > MAX_FIELD_LENGTH
                ? value.substring(0, MAX_FIELD_LENGTH) + "..."
                :value;

    }

    private Map<String, Object> fetchFromOpenFda(String drugName){
        return openFdaClient.get()
                .uri("/drug/label.json?search=openfda.generic_name:{name}&limit=1", drugName)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }


    private DrugInfo mapToDrugInfo(Map<String, Object> response, String drugName){

        if(!(response.get("results") instanceof List<?> results) || response.isEmpty()){
            log.info("OpenFDA no encontró resultados para: {}", drugName);
            return null;
        }

        if( !(results.getFirst() instanceof Map<?,?> result) ){
            return null;
        }

        var openFda = result.get("openfda") instanceof Map<?, ?> m ? m : Map.of();

        return new DrugInfo(
                extractFirst(openFda, "brand_name"),
                extractFirst(openFda, "generic_name"),
                extractFirst(result, "purpose"),
                extractFirst(result, "warnings"),
                extractFirst(result, "dosage_and_administration"));

    }


}
















