import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, ApiError, type Patient, type PatientInput } from '../api'
import { formatDate } from '../format'
import PatientBilling from './PatientBilling'
import PatientForm from './PatientForm'
import { Alert, Button, Modal, Toast, useNotice } from './ui'

type Dialog =
  | { kind: 'create' }
  | { kind: 'view'; patient: Patient }
  | { kind: 'edit'; patient: Patient }
  | { kind: 'delete'; patient: Patient }
  | null

export default function PatientsPage({ onSignOut }: { onSignOut: () => void }) {
  const [patients, setPatients] = useState<Patient[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [dialog, setDialog] = useState<Dialog>(null)
  const [notice, setNotice] = useNotice()

  // Any 401 means the token expired or was rejected: go back to the login screen.
  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onSignOut()
      setError(err instanceof ApiError ? err.message : 'Could not reach the server.')
    },
    [onSignOut],
  )

  const load = useCallback(async () => {
    setLoading(true)
    setError('')
    try {
      const list = await api.listPatients()
      setPatients(list.sort((a, b) => a.name.localeCompare(b.name)))
    } catch (err) {
      handleError(err)
    } finally {
      setLoading(false)
    }
  }, [handleError])

  useEffect(() => {
    load()
  }, [load])

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return patients
    return patients.filter((p) =>
      [p.name, p.email, p.address].some((v) => v.toLowerCase().includes(q)),
    )
  }, [patients, query])

  const openView = async (patient: Patient) => {
    setDialog({ kind: 'view', patient })
    try {
      setDialog({ kind: 'view', patient: await api.getPatient(patient.id) })
    } catch (err) {
      setDialog(null)
      handleError(err)
    }
  }

  // Form errors (400) are shown inside the form; only a 401 leaves the page.
  const submitForm = async (run: () => Promise<Patient>, done: string) => {
    try {
      await run()
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) return onSignOut()
      throw err
    }
    setDialog(null)
    setNotice(done)
    await load()
  }

  const create = (input: PatientInput) => submitForm(() => api.createPatient(input), 'Patient added')

  const update = (id: string) => (input: PatientInput) =>
    submitForm(() => api.updatePatient(id, input), 'Changes saved')

  const remove = async (patient: Patient) => {
    try {
      await api.deletePatient(patient.id)
      setDialog(null)
      setNotice(`${patient.name} deleted`)
      await load()
    } catch (err) {
      setDialog(null)
      handleError(err)
    }
  }

  return (
    <>
      <main className="mx-auto max-w-6xl space-y-4 px-4 py-6">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h1 className="text-xl font-semibold">Patients</h1>
            <p className="text-sm text-slate-500">
              {loading ? 'Loading…' : `${filtered.length} of ${patients.length} shown`}
            </p>
          </div>
          <div className="flex flex-col gap-2 sm:flex-row">
            <input
              type="search"
              placeholder="Search name, email or address"
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-200 sm:w-72"
            />
            <Button onClick={() => setDialog({ kind: 'create' })}>+ Add patient</Button>
          </div>
        </div>

        {error && <Alert>{error}</Alert>}

        {!loading && filtered.length === 0 && !error && (
          <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
            {patients.length === 0 ? 'No patients yet. Add the first one.' : 'No patients match your search.'}
          </div>
        )}

        {filtered.length > 0 && (
          <>
            {/* Phones: cards */}
            <ul className="space-y-3 md:hidden">
              {filtered.map((p) => (
                <li key={p.id} className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
                  <button className="w-full text-left" onClick={() => openView(p)}>
                    <div className="font-medium">{p.name}</div>
                    <div className="truncate text-sm text-slate-500">{p.email}</div>
                    <div className="mt-1 text-sm text-slate-500">Born {formatDate(p.dateOfBirth)}</div>
                  </button>
                  <div className="mt-3 flex gap-2">
                    <Button variant="secondary" className="flex-1" onClick={() => setDialog({ kind: 'edit', patient: p })}>
                      Edit
                    </Button>
                    <Button variant="secondary" className="flex-1 text-red-600!" onClick={() => setDialog({ kind: 'delete', patient: p })}>
                      Delete
                    </Button>
                  </div>
                </li>
              ))}
            </ul>

            {/* Tablets and up: table */}
            <div className="hidden overflow-hidden rounded-xl bg-white shadow-sm ring-1 ring-slate-200 md:block">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-4 py-3 font-medium">Name</th>
                    <th className="px-4 py-3 font-medium">Email</th>
                    <th className="px-4 py-3 font-medium">Address</th>
                    <th className="px-4 py-3 font-medium">Date of birth</th>
                    <th className="px-4 py-3" />
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {filtered.map((p) => (
                    <tr key={p.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3">
                        <button className="font-medium text-teal-700 hover:underline" onClick={() => openView(p)}>
                          {p.name}
                        </button>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{p.email}</td>
                      <td className="max-w-xs truncate px-4 py-3 text-slate-600">{p.address}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatDate(p.dateOfBirth)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right">
                        <button
                          className="rounded-md px-2 py-1 text-slate-600 hover:bg-slate-100"
                          onClick={() => setDialog({ kind: 'edit', patient: p })}
                        >
                          Edit
                        </button>
                        <button
                          className="rounded-md px-2 py-1 text-red-600 hover:bg-red-50"
                          onClick={() => setDialog({ kind: 'delete', patient: p })}
                        >
                          Delete
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </>
        )}
      </main>

      <Toast message={notice} />

      {dialog?.kind === 'create' && (
        <Modal title="Add patient" onClose={() => setDialog(null)}>
          <PatientForm onSubmit={create} onCancel={() => setDialog(null)} />
        </Modal>
      )}

      {dialog?.kind === 'edit' && (
        <Modal title="Edit patient" onClose={() => setDialog(null)}>
          <PatientForm patient={dialog.patient} onSubmit={update(dialog.patient.id)} onCancel={() => setDialog(null)} />
        </Modal>
      )}

      {dialog?.kind === 'view' && (
        <Modal title={dialog.patient.name} onClose={() => setDialog(null)} wide>
          <dl className="grid gap-3 text-sm sm:grid-cols-2">
            {[
              ['Email', dialog.patient.email],
              ['Address', dialog.patient.address],
              ['Date of birth', formatDate(dialog.patient.dateOfBirth)],
              ['Patient ID', dialog.patient.id],
            ].map(([label, value]) => (
              <div key={label}>
                <dt className="text-slate-500">{label}</dt>
                <dd className="break-all font-medium">{value}</dd>
              </div>
            ))}
          </dl>
          <PatientBilling patient={dialog.patient} onNotice={setNotice} onSignOut={onSignOut} />
          <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setDialog(null)}>
              Close
            </Button>
            <Button onClick={() => setDialog({ kind: 'edit', patient: dialog.patient })}>Edit patient</Button>
          </div>
        </Modal>
      )}

      {dialog?.kind === 'delete' && (
        <Modal title="Delete patient?" onClose={() => setDialog(null)}>
          <p className="text-sm text-slate-600">
            This permanently removes <span className="font-medium text-slate-900">{dialog.patient.name}</span>.
            Their billing account will be closed; existing invoices are kept.
          </p>
          <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button variant="secondary" onClick={() => setDialog(null)}>
              Cancel
            </Button>
            <Button variant="danger" onClick={() => remove(dialog.patient)}>
              Delete
            </Button>
          </div>
        </Modal>
      )}
    </>
  )
}
