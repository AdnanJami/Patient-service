export interface Patient {
  id: string
  name: string
  email: string
  address: string
  dateOfBirth: string
}

export interface PatientInput {
  name: string
  email: string
  address: string
  dateOfBirth: string
  registeredDate?: string
}

export interface BillingAccount {
  id: string
  patientId: string
  name: string
  email: string
  status: string
  createdAt: string
}

export type InvoiceStatus = 'UNPAID' | 'PAID'

export interface Invoice {
  id: string
  patientId: string
  patientName: string
  /** 'CLOSED' once the patient has been deleted. */
  accountStatus: string
  description: string
  amount: number
  status: InvoiceStatus
  issuedDate: string
  dueDate: string
  paidDate: string | null
}

export interface InvoiceInput {
  description: string
  amount: string
  dueDate: string
}

/** Field name -> message, as returned by the patient and billing services on 400. */
export type FieldErrors = Record<string, string>

export class ApiError extends Error {
  status: number
  fieldErrors: FieldErrors

  constructor(status: number, message: string, fieldErrors: FieldErrors = {}) {
    super(message)
    this.status = status
    this.fieldErrors = fieldErrors
  }
}

const TOKEN_KEY = 'token'

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const token = tokenStore.get()
  if (token) headers.set('Authorization', `Bearer ${token}`)
  if (init.body) headers.set('Content-Type', 'application/json')

  const res = await fetch(path, { ...init, headers })

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    if (res.status === 401) {
      throw new ApiError(401, 'Your session has expired. Please sign in again.')
    }
    const { message, ...fields } = body as FieldErrors
    throw new ApiError(res.status, message ?? `Request failed (${res.status})`, fields)
  }
  // Some endpoints (e.g. /auth/validate, DELETE) reply with no body.
  const text = await res.text()
  return (text ? JSON.parse(text) : undefined) as T
}

export const api = {
  async login(email: string, password: string): Promise<string> {
    try {
      const { token } = await request<{ token: string }>('/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      })
      return token
    } catch (e) {
      if (e instanceof ApiError && e.status === 401) {
        throw new ApiError(401, 'Invalid email or password.')
      }
      throw e
    }
  },

  async validate(): Promise<boolean> {
    try {
      await request<void>('/auth/validate')
      return true
    } catch {
      return false
    }
  },

  listPatients: () => request<Patient[]>('/api/patients'),

  getPatient: (id: string) => request<Patient>(`/api/patients/${id}`),

  createPatient: (input: PatientInput) =>
    request<Patient>('/api/patients', { method: 'POST', body: JSON.stringify(input) }),

  updatePatient: (id: string, input: PatientInput) =>
    request<Patient>(`/api/patients/${id}`, { method: 'PUT', body: JSON.stringify(input) }),

  deletePatient: (id: string) => request<void>(`/api/patients/${id}`, { method: 'DELETE' }),

  /** Resolves to null when the patient has no billing account yet. */
  async getBillingAccount(patientId: string): Promise<BillingAccount | null> {
    try {
      return await request<BillingAccount>(`/api/billing/accounts/${patientId}`)
    } catch (e) {
      if (e instanceof ApiError && e.status === 404) return null
      throw e
    }
  },

  createBillingAccount: (patient: Patient) =>
    request<BillingAccount>('/api/billing/accounts', {
      method: 'POST',
      body: JSON.stringify({ patientId: patient.id, name: patient.name, email: patient.email }),
    }),

  listPatientInvoices: (patientId: string) => request<Invoice[]>(`/api/billing/accounts/${patientId}/invoices`),

  createInvoice: (patientId: string, input: InvoiceInput) =>
    request<Invoice>(`/api/billing/accounts/${patientId}/invoices`, {
      method: 'POST',
      body: JSON.stringify(input),
    }),

  listInvoices: () => request<Invoice[]>('/api/billing/invoices'),

  payInvoice: (invoiceId: string) => request<Invoice>(`/api/billing/invoices/${invoiceId}/pay`, { method: 'POST' }),
}
