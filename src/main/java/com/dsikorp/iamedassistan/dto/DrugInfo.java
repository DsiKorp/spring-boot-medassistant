package com.dsikorp.iamedassistan.dto;

public record DrugInfo(
        String brandName,
        String genericName,
        String purpose,
        String warnings,
        String dosageAndAdministration
) {
}
