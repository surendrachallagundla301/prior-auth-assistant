package com.surendra.priorauth.domain;

/** Supporting documents a payer may require before approving a prior-authorization request. */
public enum DocumentType {
    PHYSICIAN_ORDER,
    CLINICAL_NOTES,
    IMAGING_REPORT,
    LAB_RESULTS,
    PRIOR_TREATMENT_HISTORY,
    MEDICAL_NECESSITY_LETTER
}
