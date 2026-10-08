import type { CheckResult, CreatedRequest, RequestForm } from './types';
import { parseDiagnosisCodes } from './validation';

const BASE_URL = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

function authHeader(username: string, password: string): string {
  return 'Basic ' + btoa(`${username}:${password}`);
}

async function call<T>(path: string, init: RequestInit, auth: string): Promise<T> {
  const response = await fetch(`${BASE_URL}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', Authorization: auth, ...init.headers },
  });
  if (!response.ok) {
    const body = await response.json().catch(() => ({}));
    const detail = Array.isArray(body.errors) ? body.errors.join('; ') : body.detail;
    throw new Error(detail || `Request failed with status ${response.status}`);
  }
  return response.json() as Promise<T>;
}

export async function submitAndCheck(
  form: RequestForm,
  username: string,
  password: string,
): Promise<{ request: CreatedRequest; result: CheckResult }> {
  const auth = authHeader(username, password);
  const request = await call<CreatedRequest>(
    '/api/requests',
    {
      method: 'POST',
      body: JSON.stringify({
        ...form,
        memberId: form.memberId.trim().toUpperCase(),
        cptCode: form.cptCode.trim().toUpperCase(),
        icd10Codes: parseDiagnosisCodes(form.icd10Codes),
      }),
    },
    auth,
  );
  const result = await call<CheckResult>(`/api/requests/${request.id}/check`, { method: 'POST' }, auth);
  return { request, result };
}
