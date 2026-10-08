package com.surendra.priorauth.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surendra.priorauth.domain.DocumentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Checks a prior-authorization request against payer documentation rules before it is sent,
 * so gaps are fixed upstream instead of coming back as denials.
 */
@Component
public class DocumentationRulesEngine {

    /** ICD-10-CM: letter, digit, letter-or-digit, then an optional dot and 1-4 more characters. */
    private static final Pattern ICD10 = Pattern.compile("^[A-TV-Z]\\d[A-Z0-9](\\.[A-Z0-9]{1,4})?$");
    /** CPT: five digits. HCPCS Level II: a letter followed by four digits. */
    private static final Pattern PROCEDURE_CODE = Pattern.compile("^(\\d{5}|[A-V]\\d{4})$");

    private final DocumentationRules rules;

    @Autowired
    public DocumentationRulesEngine(ObjectMapper mapper) {
        this(load(mapper));
    }

    DocumentationRulesEngine(DocumentationRules rules) {
        this.rules = rules;
    }

    private static DocumentationRules load(ObjectMapper mapper) {
        try (InputStream in = new ClassPathResource("rules/documentation-rules.json").getInputStream()) {
            return mapper.readValue(in, DocumentationRules.class);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not load documentation rules", e);
        }
    }

    public DocumentationRules rules() {
        return rules;
    }

    public CheckResult check(String payer, String procedureCode, List<String> diagnosisCodes,
                             Set<DocumentType> submitted) {
        List<String> issues = new ArrayList<>();
        String code = procedureCode == null ? "" : procedureCode.trim().toUpperCase(Locale.ROOT);

        if (!PROCEDURE_CODE.matcher(code).matches()) {
            issues.add("Procedure code '" + procedureCode + "' is not a valid CPT/HCPCS code");
        }

        if (diagnosisCodes == null || diagnosisCodes.isEmpty()) {
            issues.add("At least one ICD-10 diagnosis code is required");
        } else {
            for (String dx : diagnosisCodes) {
                String normalised = dx.trim().toUpperCase(Locale.ROOT);
                if (!ICD10.matcher(normalised).matches()) {
                    issues.add("Diagnosis code '" + dx + "' is not a valid ICD-10-CM code");
                }
            }
        }

        Set<DocumentType> required = EnumSet.noneOf(DocumentType.class);
        DocumentationRules.Procedure procedure = rules.procedures().get(code);
        if (procedure == null) {
            issues.add("No documentation rules for procedure " + code + "; route to manual review");
        } else {
            required.addAll(procedure.required());
            Map<String, List<DocumentType>> overrides =
                    rules.payerOverrides().getOrDefault(payer, Map.of());
            required.addAll(overrides.getOrDefault(code, List.of()));
        }

        Set<DocumentType> missing = EnumSet.noneOf(DocumentType.class);
        missing.addAll(required);
        missing.removeAll(submitted == null ? Set.of() : submitted);
        for (DocumentType doc : missing) {
            issues.add("Missing required document: " + doc);
        }

        DenialRisk risk = assessRisk(missing.size(), issues.size() - missing.size(), procedure == null);
        boolean ready = issues.isEmpty();
        return new CheckResult(ready, required, missing, risk, List.copyOf(issues));
    }

    private static DenialRisk assessRisk(int missingDocs, int codeIssues, boolean unknownProcedure) {
        if (codeIssues > 0 || unknownProcedure || missingDocs >= 2) {
            return DenialRisk.HIGH;
        }
        return missingDocs == 1 ? DenialRisk.MEDIUM : DenialRisk.LOW;
    }
}
