package com.surendra.priorauth.rules;

import com.surendra.priorauth.domain.DocumentType;

import java.util.List;
import java.util.Map;

/** Shape of {@code rules/documentation-rules.json}. */
public record DocumentationRules(
        Map<String, Procedure> procedures,
        Map<String, Map<String, List<DocumentType>>> payerOverrides) {

    public record Procedure(String description, List<DocumentType> required) {
    }
}
