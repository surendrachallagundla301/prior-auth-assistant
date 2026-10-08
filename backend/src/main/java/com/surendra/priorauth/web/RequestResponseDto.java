package com.surendra.priorauth.web;

import com.surendra.priorauth.domain.DocumentType;
import com.surendra.priorauth.domain.PriorAuthRequest;
import com.surendra.priorauth.domain.RequestStatus;
import com.surendra.priorauth.security.PhiMasker;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** API view of a request. PHI is always masked: the API never returns full identifiers. */
public record RequestResponseDto(
        Long id,
        String patientName,
        String memberId,
        String dateOfBirth,
        String payer,
        String cptCode,
        List<String> icd10Codes,
        Set<DocumentType> submittedDocuments,
        RequestStatus status,
        String createdBy,
        Instant createdAt) {

    static RequestResponseDto from(PriorAuthRequest r) {
        return new RequestResponseDto(
                r.getId(),
                PhiMasker.maskName(r.getPatientName()),
                PhiMasker.maskMemberId(r.getMemberId()),
                PhiMasker.maskDate(r.getDateOfBirth()),
                r.getPayer(),
                r.getCptCode(),
                Arrays.asList(r.getIcd10Codes().split(",")),
                r.getSubmittedDocuments(),
                r.getStatus(),
                r.getCreatedBy(),
                r.getCreatedAt());
    }
}
