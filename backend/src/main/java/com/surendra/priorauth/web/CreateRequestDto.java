package com.surendra.priorauth.web;

import com.surendra.priorauth.domain.DocumentType;
import jakarta.validation.constraints.*;

import java.util.List;
import java.util.Set;

public record CreateRequestDto(
        @NotBlank @Size(max = 120) String patientName,
        @NotBlank @Pattern(regexp = "^[A-Z]{3}\\d{6,12}$", message = "must be 3 letters followed by 6-12 digits")
        String memberId,
        @NotBlank @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "must be YYYY-MM-DD") String dateOfBirth,
        @NotBlank String payer,
        @NotBlank String cptCode,
        @NotEmpty List<@NotBlank String> icd10Codes,
        @NotNull Set<DocumentType> submittedDocuments) {
}
