package com.surendra.priorauth.web;

import com.surendra.priorauth.domain.AuditEvent;
import com.surendra.priorauth.rules.CheckResult;
import com.surendra.priorauth.rules.DocumentationRules;
import com.surendra.priorauth.rules.DocumentationRulesEngine;
import com.surendra.priorauth.service.AuditService;
import com.surendra.priorauth.service.PriorAuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class PriorAuthController {

    private final PriorAuthService service;
    private final AuditService audit;
    private final DocumentationRulesEngine rulesEngine;

    public PriorAuthController(PriorAuthService service, AuditService audit, DocumentationRulesEngine rulesEngine) {
        this.service = service;
        this.audit = audit;
        this.rulesEngine = rulesEngine;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public RequestResponseDto create(@Valid @RequestBody CreateRequestDto dto, Authentication auth) {
        return RequestResponseDto.from(service.create(dto.patientName(), dto.memberId(), dto.dateOfBirth(),
                dto.payer(), dto.cptCode(), dto.icd10Codes(), dto.submittedDocuments(), auth.getName()));
    }

    @GetMapping("/requests/{id}")
    public RequestResponseDto get(@PathVariable Long id, Authentication auth) {
        return RequestResponseDto.from(service.get(id, auth.getName()));
    }

    @PostMapping("/requests/{id}/check")
    public CheckResult check(@PathVariable Long id, Authentication auth) {
        return service.check(id, auth.getName());
    }

    @GetMapping("/rules")
    public DocumentationRules rules() {
        return rulesEngine.rules();
    }

    @GetMapping("/audit")
    public List<AuditEvent> audit(@RequestParam(required = false) Long requestId) {
        return requestId == null ? audit.all() : audit.forRequest(requestId);
    }
}
