package com.surendra.priorauth.domain;

import jakarta.persistence.*;

import java.time.Instant;

/** Append-only record of who did what to which request. Never stores PHI. */
@Entity
@Table(name = "audit_event")
public class AuditEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String action;

    @Column(name = "request_id")
    private Long requestId;

    @Column(length = 1000)
    private String details;

    protected AuditEvent() {
    }

    public AuditEvent(String actor, String action, Long requestId, String details) {
        this.actor = actor;
        this.action = action;
        this.requestId = requestId;
        this.details = details;
    }

    public Long getId() { return id; }
    public Instant getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getAction() { return action; }
    public Long getRequestId() { return requestId; }
    public String getDetails() { return details; }
}
