import type { RequestForm } from './types';

const ICD10 = /^[A-TV-Z]\d[A-Z0-9](\.[A-Z0-9]{1,4})?$/;
const PROCEDURE = /^(\d{5}|[A-V]\d{4})$/;
const MEMBER_ID = /^[A-Z]{3}\d{6,12}$/;
const ISO_DATE = /^\d{4}-\d{2}-\d{2}$/;

/** Splits "M17.11, m54.5" into ["M17.11", "M54.5"]. */
export function parseDiagnosisCodes(input: string): string[] {
  return input
    .split(/[\s,]+/)
    .map((code) => code.trim().toUpperCase())
    .filter((code) => code.length > 0);
}

/**
 * Client-side checks that mirror the API's validation, so users see mistakes before
 * submitting. The server re-validates everything; this is only for faster feedback.
 */
export function validateForm(form: RequestForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (!form.patientName.trim()) errors.patientName = 'Patient name is required';
  if (!MEMBER_ID.test(form.memberId.trim().toUpperCase())) {
    errors.memberId = 'Member ID must be 3 letters followed by 6-12 digits';
  }
  if (!ISO_DATE.test(form.dateOfBirth)) errors.dateOfBirth = 'Date of birth must be YYYY-MM-DD';
  if (!form.payer) errors.payer = 'Select a payer';
  if (!PROCEDURE.test(form.cptCode.trim().toUpperCase())) {
    errors.cptCode = 'Enter a 5-digit CPT code or a HCPCS code like J0897';
  }
  const codes = parseDiagnosisCodes(form.icd10Codes);
  if (codes.length === 0) {
    errors.icd10Codes = 'Enter at least one ICD-10 code';
  } else {
    const bad = codes.filter((code) => !ICD10.test(code));
    if (bad.length > 0) errors.icd10Codes = `Invalid ICD-10 code(s): ${bad.join(', ')}`;
  }
  return errors;
}

/** Turns PRIOR_TREATMENT_HISTORY into "Prior treatment history". */
export function humanize(value: string): string {
  const words = value.toLowerCase().split('_');
  return words.map((w, i) => (i === 0 ? w.charAt(0).toUpperCase() + w.slice(1) : w)).join(' ');
}
