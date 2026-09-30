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

/** Field name -> message, as returned by the patient service on 400. */
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
}
