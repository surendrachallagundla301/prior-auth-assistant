package com.surendra.priorauth.domain;

import com.surendra.priorauth.security.EncryptedStringConverter;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

/**
 * A prior-authorization request. Fields that identify a patient (PHI) are encrypted at rest
 * with AES-GCM through {@link EncryptedStringConverter}.
 */
@Entity
@Table(name = "prior_auth_request")
public class PriorAuthRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "patient_name", nullable = false, length = 512)
    private String patientName;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "member_id", nullable = false, length = 512)
    private String memberId;

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "date_of_birth", nullable = false, length = 512)
    private String dateOfBirth;

    @Column(nullable = false)
    private String payer;

    @Column(name = "cpt_code", nullable = false)
    private String cptCode;

    /** Comma-separated ICD-10-CM diagnosis codes. Not PHI on their own. */
    @Column(name = "icd10_codes", nullable = false)
    private String icd10Codes;

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "submitted_document", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "document_type")
    private Set<DocumentType> submittedDocuments = EnumSet.noneOf(DocumentType.class);

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.SUBMITTED;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PriorAuthRequest() {
    }

    public PriorAuthRequest(String patientName, String memberId, String dateOfBirth, String payer,
                            String cptCode, String icd10Codes, Set<DocumentType> submittedDocuments,
                            String createdBy) {
        this.patientName = patientName;
        this.memberId = memberId;
        this.dateOfBirth = dateOfBirth;
        this.payer = payer;
        this.cptCode = cptCode;
        this.icd10Codes = icd10Codes;
        this.submittedDocuments = submittedDocuments.isEmpty()
                ? EnumSet.noneOf(DocumentType.class) : EnumSet.copyOf(submittedDocuments);
        this.createdBy = createdBy;
    }

    public Long getId() { return id; }
    public String getPatientName() { return patientName; }
    public String getMemberId() { return memberId; }
    public String getDateOfBirth() { return dateOfBirth; }
    public String getPayer() { return payer; }
    public String getCptCode() { return cptCode; }
    public String getIcd10Codes() { return icd10Codes; }
    public Set<DocumentType> getSubmittedDocuments() { return submittedDocuments; }
    public RequestStatus getStatus() { return status; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(RequestStatus status) { this.status = status; }
}
