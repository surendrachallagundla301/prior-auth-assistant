package com.surendra.priorauth.domain;

public enum RequestStatus {
    /** Created but not yet checked. */
    SUBMITTED,
    /** All required documentation is present; safe to send to the payer. */
    READY_FOR_SUBMISSION,
    /** One or more required documents are missing; sending now would likely be denied. */
    NEEDS_DOCUMENTATION
}
