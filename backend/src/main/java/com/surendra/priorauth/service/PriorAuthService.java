package com.surendra.priorauth.service;

import com.surendra.priorauth.domain.*;
import com.surendra.priorauth.rules.CheckResult;
import com.surendra.priorauth.rules.DocumentationRulesEngine;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
public class PriorAuthService {

    private final PriorAuthRequestRepository repository;
    private final DocumentationRulesEngine rulesEngine;
    private final AuditService audit;

    public PriorAuthService(PriorAuthRequestRepository repository, DocumentationRulesEngine rulesEngine,
                            AuditService audit) {
        this.repository = repository;
        this.rulesEngine = rulesEngine;
        this.audit = audit;
    }

    @Transactional
    public PriorAuthRequest create(String patientName, String memberId, String dateOfBirth, String payer,
                                   String cptCode, List<String> icd10Codes, Set<DocumentType> documents,
                                   String actor) {
        PriorAuthRequest request = new PriorAuthRequest(patientName, memberId, dateOfBirth, payer,
                cptCode.trim().toUpperCase(), String.join(",", icd10Codes), documents, actor);
        PriorAuthRequest saved = repository.save(request);
        audit.record(actor, "REQUEST_CREATED", saved.getId(),
                "payer=" + payer + " cpt=" + saved.getCptCode() + " documents=" + documents.size());
        return saved;
    }

    @Transactional
    public PriorAuthRequest get(Long id, String actor) {
        PriorAuthRequest request = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Request " + id + " not found"));
        audit.record(actor, "REQUEST_VIEWED", id, null);
        return request;
    }

    @Transactional
    public CheckResult check(Long id, String actor) {
        PriorAuthRequest request = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Request " + id + " not found"));
        List<String> diagnoses = Arrays.stream(request.getIcd10Codes().split(","))
                .filter(s -> !s.isBlank()).toList();
        CheckResult result = rulesEngine.check(request.getPayer(), request.getCptCode(), diagnoses,
                request.getSubmittedDocuments());
        request.setStatus(result.readyForSubmission()
                ? RequestStatus.READY_FOR_SUBMISSION : RequestStatus.NEEDS_DOCUMENTATION);
        audit.record(actor, "DOCUMENTATION_CHECKED", id,
                "ready=" + result.readyForSubmission() + " risk=" + result.denialRisk()
                        + " missing=" + result.missingDocuments());
        return result;
    }
}
