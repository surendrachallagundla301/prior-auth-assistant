package com.surendra.priorauth.rules;

import com.surendra.priorauth.domain.DocumentType;

import java.util.List;
import java.util.Set;

/**
 * Outcome of a pre-submission documentation check.
 *
 * @param readyForSubmission true when nothing is missing and every code is valid
 * @param requiredDocuments  everything the payer needs for this procedure
 * @param missingDocuments   required documents not yet attached
 * @param denialRisk         estimated denial risk if submitted now
 * @param issues             human-readable problems to fix
 */
public record CheckResult(
        boolean readyForSubmission,
        Set<DocumentType> requiredDocuments,
        Set<DocumentType> missingDocuments,
        DenialRisk denialRisk,
        List<String> issues) {
}
