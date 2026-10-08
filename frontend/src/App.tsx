import { useState } from 'react';
import { submitAndCheck } from './api';
import { DOCUMENT_TYPES, type CheckResult, type CreatedRequest, type DocumentType, type RequestForm } from './types';
import { humanize, validateForm } from './validation';

const EMPTY: RequestForm = {
  patientName: '',
  memberId: '',
  dateOfBirth: '',
  payer: '',
  cptCode: '',
  icd10Codes: '',
  submittedDocuments: [],
};

const PAYERS = [
  { id: 'ACME_HEALTH', label: 'Acme Health Plan (demo)' },
  { id: 'SUMMIT_CARE', label: 'Summit Care (demo)' },
];

export default function App() {
  const [form, setForm] = useState<RequestForm>(EMPTY);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [result, setResult] = useState<{ request: CreatedRequest; result: CheckResult } | null>(null);
  const [apiError, setApiError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const update = (field: keyof RequestForm, value: string) => setForm({ ...form, [field]: value });

  const toggleDocument = (doc: DocumentType) =>
    setForm({
      ...form,
      submittedDocuments: form.submittedDocuments.includes(doc)
        ? form.submittedDocuments.filter((d) => d !== doc)
        : [...form.submittedDocuments, doc],
    });

  async function onSubmit(event: React.FormEvent) {
    event.preventDefault();
    const found = validateForm(form);
    setErrors(found);
    setApiError(null);
    if (Object.keys(found).length > 0) return;
    setBusy(true);
    try {
      // Demo credentials for the INTAKE role; a real deployment would sign in through SSO.
      setResult(await submitAndCheck(form, 'intake', 'intake-demo'));
    } catch (e) {
      setResult(null);
      setApiError(e instanceof Error ? e.message : 'Something went wrong');
    } finally {
      setBusy(false);
    }
  }

  const field = (name: keyof RequestForm, label: string, placeholder = '') => (
    <label>
      {label}
      <input value={form[name] as string} placeholder={placeholder} onChange={(e) => update(name, e.target.value)} />
      {errors[name] && <span className="error">{errors[name]}</span>}
    </label>
  );

  return (
    <main>
      <h1>Prior Authorization Assistant</h1>
      <p className="lede">
        Check a prior-authorization request for missing documentation before it goes to the payer. Use synthetic data
        only.
      </p>

      <form onSubmit={onSubmit} noValidate>
        {field('patientName', 'Patient name', 'Jane Doe')}
        {field('memberId', 'Member ID', 'ABC123456789')}
        {field('dateOfBirth', 'Date of birth', 'YYYY-MM-DD')}
        <label>
          Payer
          <select value={form.payer} onChange={(e) => update('payer', e.target.value)}>
            <option value="">Select a payer</option>
            {PAYERS.map((p) => (
              <option key={p.id} value={p.id}>
                {p.label}
              </option>
            ))}
          </select>
          {errors.payer && <span className="error">{errors.payer}</span>}
        </label>
        {field('cptCode', 'Procedure code (CPT/HCPCS)', '27447')}
        {field('icd10Codes', 'Diagnosis codes (ICD-10)', 'M17.11')}

        <fieldset>
          <legend>Attached documents</legend>
          {DOCUMENT_TYPES.map((doc) => (
            <label key={doc} className="check">
              <input
                type="checkbox"
                checked={form.submittedDocuments.includes(doc)}
                onChange={() => toggleDocument(doc)}
              />
              {humanize(doc)}
            </label>
          ))}
        </fieldset>

        <button type="submit" disabled={busy}>
          {busy ? 'Checking…' : 'Submit and check'}
        </button>
      </form>

      {apiError && <p className="error banner">{apiError}</p>}

      {result && (
        <section className={`result risk-${result.result.denialRisk.toLowerCase()}`}>
          <h2>{result.result.readyForSubmission ? 'Ready for submission' : 'Needs documentation'}</h2>
          <p>
            Request #{result.request.id} for {result.request.patientName} (member {result.request.memberId}) · Denial
            risk: <strong>{result.result.denialRisk}</strong>
          </p>
          {result.result.issues.length > 0 && (
            <ul>
              {result.result.issues.map((issue) => (
                <li key={issue}>{issue}</li>
              ))}
            </ul>
          )}
        </section>
      )}
    </main>
  );
}
