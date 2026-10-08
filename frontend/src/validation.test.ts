import { humanize, parseDiagnosisCodes, validateForm } from './validation';
import type { RequestForm } from './types';

const valid: RequestForm = {
  patientName: 'Jane Doe',
  memberId: 'ABC123456789',
  dateOfBirth: '1985-04-12',
  payer: 'ACME_HEALTH',
  cptCode: '27447',
  icd10Codes: 'M17.11',
  submittedDocuments: ['PHYSICIAN_ORDER'],
};

describe('parseDiagnosisCodes', () => {
  it('splits on commas and spaces and uppercases', () => {
    expect(parseDiagnosisCodes('m17.11, M54.50  r51.9')).toEqual(['M17.11', 'M54.50', 'R51.9']);
  });

  it('returns an empty list for blank input', () => {
    expect(parseDiagnosisCodes('  ,  ')).toEqual([]);
  });
});

describe('validateForm', () => {
  it('accepts a valid request', () => {
    expect(validateForm(valid)).toEqual({});
  });

  it('accepts HCPCS procedure codes', () => {
    expect(validateForm({ ...valid, cptCode: 'J0897' })).toEqual({});
  });

  it('flags a bad member ID and date', () => {
    const errors = validateForm({ ...valid, memberId: '12345', dateOfBirth: '04/12/1985' });
    expect(Object.keys(errors).sort()).toEqual(['dateOfBirth', 'memberId']);
  });

  it('lists each invalid diagnosis code', () => {
    const errors = validateForm({ ...valid, icd10Codes: 'M17.11, XYZ, 123' });
    expect(errors.icd10Codes).toBe('Invalid ICD-10 code(s): XYZ, 123');
  });

  it('requires at least one diagnosis code', () => {
    expect(validateForm({ ...valid, icd10Codes: '' }).icd10Codes).toBeDefined();
  });
});

describe('humanize', () => {
  it('formats enum values for display', () => {
    expect(humanize('PRIOR_TREATMENT_HISTORY')).toBe('Prior treatment history');
  });
});
