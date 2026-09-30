import { useState, type FormEvent } from 'react'
import { ApiError, type FieldErrors, type Patient, type PatientInput } from '../api'
import { today } from '../format'
import { Alert, Button, Field } from './ui'

export default function PatientForm({
  patient,
  onSubmit,
  onCancel,
}: {
  /** Existing patient to edit; omitted when creating. */
  patient?: Patient
  onSubmit: (input: PatientInput) => Promise<void>
  onCancel: () => void
}) {
  const [values, setValues] = useState<PatientInput>({
    name: patient?.name ?? '',
    email: patient?.email ?? '',
    address: patient?.address ?? '',
    dateOfBirth: patient?.dateOfBirth ?? '',
    registeredDate: patient ? undefined : today(),
  })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key: keyof PatientInput) => (e: { target: { value: string } }) =>
    setValues((v) => ({ ...v, [key]: e.target.value }))

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    setBusy(true)
    setErrors({})
    setMessage('')
    try {
      await onSubmit(values)
    } catch (err) {
      if (err instanceof ApiError) {
        setErrors(err.fieldErrors)
        if (Object.keys(err.fieldErrors).length === 0) setMessage(err.message)
      } else {
        setMessage('Could not reach the server.')
      }
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} className="space-y-4">
      {message && <Alert>{message}</Alert>}
      <Field label="Full name" required maxLength={100} value={values.name} onChange={set('name')} error={errors.name} />
      <Field label="Email" type="email" required value={values.email} onChange={set('email')} error={errors.email} />
      <Field label="Address" required value={values.address} onChange={set('address')} error={errors.address} />
      <div className="grid gap-4 sm:grid-cols-2">
        <Field
          label="Date of birth"
          type="date"
          required
          max={today()}
          value={values.dateOfBirth}
          onChange={set('dateOfBirth')}
          error={errors.dateOfBirth}
        />
        {!patient && (
          <Field
            label="Registered on"
            type="date"
            required
            value={values.registeredDate}
            onChange={set('registeredDate')}
            error={errors.registeredDate}
          />
        )}
      </div>
      <div className="flex flex-col-reverse gap-2 pt-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : patient ? 'Save changes' : 'Add patient'}
        </Button>
      </div>
    </form>
  )
}
