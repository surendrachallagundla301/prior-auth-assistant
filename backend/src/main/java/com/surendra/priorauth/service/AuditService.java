package com.surendra.priorauth.service;

import com.surendra.priorauth.domain.AuditEvent;
import com.surendra.priorauth.domain.AuditEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditEventRepository repository;

    public AuditService(AuditEventRepository repository) {
        this.repository = repository;
    }

    /** Records an action. {@code details} must never contain PHI. */
    public void record(String actor, String action, Long requestId, String details) {
        repository.save(new AuditEvent(actor, action, requestId, details));
        log.info("audit actor={} action={} requestId={} details={}", actor, action, requestId, details);
    }

    public List<AuditEvent> all() {
        return repository.findAll();
    }

    public List<AuditEvent> forRequest(Long requestId) {
        return repository.findByRequestIdOrderByTimestampAsc(requestId);
    }
}
