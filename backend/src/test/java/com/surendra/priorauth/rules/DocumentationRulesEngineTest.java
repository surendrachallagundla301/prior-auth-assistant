package com.surendra.priorauth.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.surendra.priorauth.domain.DocumentType;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static com.surendra.priorauth.domain.DocumentType.*;
import static org.assertj.core.api.Assertions.assertThat;

class DocumentationRulesEngineTest {

    private final DocumentationRulesEngine engine = new DocumentationRulesEngine(new ObjectMapper());

    @Test
    void completeRequestIsReadyWithLowRisk() {
        CheckResult result = engine.check("SUMMIT_CARE", "72148", List.of("M54.50"),
                EnumSet.of(PHYSICIAN_ORDER, CLINICAL_NOTES, PRIOR_TREATMENT_HISTORY));

        assertThat(result.readyForSubmission()).isTrue();
        assertThat(result.missingDocuments()).isEmpty();
        assertThat(result.denialRisk()).isEqualTo(DenialRisk.LOW);
        assertThat(result.issues()).isEmpty();
    }

    @Test
    void oneMissingDocumentIsMediumRisk() {
        CheckResult result = engine.check("SUMMIT_CARE", "72148", List.of("M54.50"),
                EnumSet.of(PHYSICIAN_ORDER, CLINICAL_NOTES));

        assertThat(result.readyForSubmission()).isFalse();
        assertThat(result.missingDocuments()).containsExactly(PRIOR_TREATMENT_HISTORY);
        assertThat(result.denialRisk()).isEqualTo(DenialRisk.MEDIUM);
    }

    @Test
    void payerOverrideAddsRequiredDocument() {
        EnumSet<DocumentType> baseDocs = EnumSet.of(PHYSICIAN_ORDER, CLINICAL_NOTES, IMAGING_REPORT,
                PRIOR_TREATMENT_HISTORY);

        CheckResult otherPayer = engine.check("SUMMIT_CARE", "27447", List.of("M17.11"), baseDocs);
        CheckResult acme = engine.check("ACME_HEALTH", "27447", List.of("M17.11"), baseDocs);

        assertThat(otherPayer.readyForSubmission()).isTrue();
        assertThat(acme.readyForSubmission()).isFalse();
        assertThat(acme.missingDocuments()).containsExactly(MEDICAL_NECESSITY_LETTER);
    }

    @Test
    void invalidCodesAreHighRiskEvenWithAllDocuments() {
        CheckResult result = engine.check("SUMMIT_CARE", "95810", List.of("not-a-code"),
                EnumSet.of(PHYSICIAN_ORDER, CLINICAL_NOTES));

        assertThat(result.readyForSubmission()).isFalse();
        assertThat(result.denialRisk()).isEqualTo(DenialRisk.HIGH);
        assertThat(result.issues()).anyMatch(i -> i.contains("not a valid ICD-10-CM code"));
    }

    @Test
    void unknownProcedureIsRoutedToManualReview() {
        CheckResult result = engine.check("SUMMIT_CARE", "99999", List.of("R51.9"), EnumSet.noneOf(DocumentType.class));

        assertThat(result.readyForSubmission()).isFalse();
        assertThat(result.denialRisk()).isEqualTo(DenialRisk.HIGH);
        assertThat(result.issues()).anyMatch(i -> i.contains("manual review"));
    }

    @Test
    void hcpcsCodesAndLowercaseInputAreAccepted() {
        CheckResult result = engine.check("SUMMIT_CARE", "j0897", List.of("m81.0"),
                EnumSet.of(PHYSICIAN_ORDER, CLINICAL_NOTES, LAB_RESULTS));

        assertThat(result.readyForSubmission()).isTrue();
    }
}
