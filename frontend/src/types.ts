export const DOCUMENT_TYPES = [
  'PHYSICIAN_ORDER',
  'CLINICAL_NOTES',
  'IMAGING_REPORT',
  'LAB_RESULTS',
  'PRIOR_TREATMENT_HISTORY',
  'MEDICAL_NECESSITY_LETTER',
] as const;

export type DocumentType = (typeof DOCUMENT_TYPES)[number];

export interface RequestForm {
  patientName: string;
  memberId: string;
  dateOfBirth: string;
  payer: string;
  cptCode: string;
  icd10Codes: string;
  submittedDocuments: DocumentType[];
}

export interface CheckResult {
  readyForSubmission: boolean;
  requiredDocuments: DocumentType[];
  missingDocuments: DocumentType[];
  denialRisk: 'LOW' | 'MEDIUM' | 'HIGH';
  issues: string[];
}

export interface CreatedRequest {
  id: number;
  patientName: string;
  memberId: string;
  status: string;
}
