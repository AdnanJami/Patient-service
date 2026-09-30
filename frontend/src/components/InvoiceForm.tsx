import { useState, type FormEvent } from 'react'
import { ApiError, type FieldErrors, type InvoiceInput } from '../api'
import { today } from '../format'
import { Alert, Button, Field } from './ui'

export default function InvoiceForm({
  onSubmit,
  onCancel,
}: {
  onSubmit: (input: InvoiceInput) => Promise<void>
  onCancel: () => void
}) {
  const [values, setValues] = useState<InvoiceInput>({ description: '', amount: '', dueDate: today(30) })
  const [errors, setErrors] = useState<FieldErrors>({})
  const [message, setMessage] = useState('')
  const [busy, setBusy] = useState(false)

  const set = (key: keyof InvoiceInput) => (e: { target: { value: string } }) =>
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
    <form onSubmit={submit} className="space-y-3 rounded-xl bg-slate-50 p-4 ring-1 ring-slate-200">
      {message && <Alert>{message}</Alert>}
      <Field
        label="Description"
        required
        maxLength={200}
        placeholder="e.g. Consultation"
        value={values.description}
        onChange={set('description')}
        error={errors.description}
      />
      <div className="grid gap-3 sm:grid-cols-2">
        <Field
          label="Amount"
          type="number"
          inputMode="decimal"
          required
          min="0.01"
          step="0.01"
          value={values.amount}
          onChange={set('amount')}
          error={errors.amount}
        />
        <Field
          label="Due date"
          type="date"
          required
          min={today()}
          value={values.dueDate}
          onChange={set('dueDate')}
          error={errors.dueDate}
        />
      </div>
      <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
        <Button type="button" variant="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : 'Create invoice'}
        </Button>
      </div>
    </form>
  )
}
