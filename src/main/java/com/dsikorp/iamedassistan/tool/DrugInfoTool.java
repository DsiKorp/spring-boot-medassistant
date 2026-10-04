package com.dsikorp.iamedassistan.tool;

import com.dsikorp.iamedassistan.dto.DrugInfo;
import com.dsikorp.iamedassistan.service.DrugInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DrugInfoTool {

    private final DrugInfoService drugInfoService;

    @Tool(description = "Buscar información oficial sobre un medicamento: " +
            "propósito, advertencias, dosis y contraindicaciones. " +
            "Usar cuando el usuario pregunte sobre un fármaco específico.")
    public DrugInfo getDrugInfo(
            @ToolParam(description = "Nombre genérico del medicamento en inglés, " +
                    "por ejemplo: ibuprofen, aspirin, metformin") String drugName
    ){
        log.info("Tool invocada — getDrugInfo: drugName={}", drugName);
        return drugInfoService.getDrugInfo(drugName);
    }

}
