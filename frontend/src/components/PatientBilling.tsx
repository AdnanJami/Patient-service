import { useCallback, useEffect, useState } from 'react'
import { api, ApiError, type BillingAccount, type Invoice, type InvoiceInput, type Patient } from '../api'
import { formatDate, formatMoney, sumAmounts } from '../format'
import InvoiceForm from './InvoiceForm'
import InvoiceStatusBadge from './InvoiceStatusBadge'
import { Alert, Button } from './ui'

/** Billing account and invoices for one patient, shown inside the patient view. */
export default function PatientBilling({
  patient,
  onNotice,
  onSignOut,
}: {
  patient: Patient
  onNotice: (message: string) => void
  onSignOut: () => void
}) {
  const [account, setAccount] = useState<BillingAccount | null>(null)
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [adding, setAdding] = useState(false)
  const [busy, setBusy] = useState(false)

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onSignOut()
      setError(err instanceof ApiError ? err.message : 'Could not reach the server.')
    },
    [onSignOut],
  )

  const load = useCallback(async () => {
    setError('')
    try {
      const acc = await api.getBillingAccount(patient.id)
      setAccount(acc)
      setInvoices(acc ? await api.listPatientInvoices(patient.id) : [])
    } catch (err) {
      handleError(err)
    } finally {
      setLoading(false)
    }
  }, [patient.id, handleError])

  useEffect(() => {
    load()
  }, [load])

  const setUpAccount = async () => {
    setBusy(true)
    try {
      await api.createBillingAccount(patient)
      onNotice('Billing account set up')
      await load()
    } catch (err) {
      handleError(err)
    } finally {
      setBusy(false)
    }
  }

  const createInvoice = async (input: InvoiceInput) => {
    try {
      await api.createInvoice(patient.id, input)
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) return onSignOut()
      throw err
    }
    setAdding(false)
    onNotice('Invoice created')
    await load()
  }

  const pay = async (invoice: Invoice) => {
    try {
      const paid = await api.payInvoice(invoice.id)
      setInvoices((list) => list.map((i) => (i.id === paid.id ? paid : i)))
      onNotice('Invoice marked as paid')
    } catch (err) {
      handleError(err)
    }
  }

  const outstanding = sumAmounts(invoices.filter((i) => i.status === 'UNPAID').map((i) => i.amount))

  return (
    <section className="mt-5 border-t border-slate-200 pt-5">
      <div className="mb-3 flex items-center justify-between gap-2">
        <h3 className="font-semibold">Billing</h3>
        {account && !adding && (
          <Button variant="secondary" className="px-3 py-1.5" onClick={() => setAdding(true)}>
            + New invoice
          </Button>
        )}
      </div>

      {error && <Alert>{error}</Alert>}

      {loading ? (
        <p className="text-sm text-slate-500">Loading billing…</p>
      ) : !account ? (
        <div className="rounded-xl border border-dashed border-slate-300 p-4 text-center text-sm text-slate-500">
          <p className="mb-3">This patient doesn't have a billing account yet.</p>
          <Button onClick={setUpAccount} disabled={busy}>
            {busy ? 'Setting up…' : 'Set up billing account'}
          </Button>
        </div>
      ) : (
        <div className="space-y-3">
          <p className="text-sm text-slate-600">
            Outstanding: <span className="font-semibold text-slate-900">{formatMoney(outstanding)}</span>
            <span className="text-slate-400"> · </span>
            {invoices.length} invoice{invoices.length === 1 ? '' : 's'}
          </p>

          {adding && <InvoiceForm onSubmit={createInvoice} onCancel={() => setAdding(false)} />}

          {invoices.length === 0 && !adding && <p className="text-sm text-slate-500">No invoices yet.</p>}

          <ul className="divide-y divide-slate-100 rounded-xl ring-1 ring-slate-200">
            {invoices.map((invoice) => (
              <li key={invoice.id} className="flex flex-wrap items-center gap-x-3 gap-y-1 px-3 py-2.5">
                <div className="min-w-0 flex-1">
                  <div className="truncate text-sm font-medium">{invoice.description}</div>
                  <div className="text-xs text-slate-500">
                    {invoice.paidDate ? `Paid ${formatDate(invoice.paidDate)}` : `Due ${formatDate(invoice.dueDate)}`}
                  </div>
                </div>
                <InvoiceStatusBadge invoice={invoice} />
                <span className="w-24 text-right text-sm font-semibold tabular-nums">{formatMoney(invoice.amount)}</span>
                {invoice.status === 'UNPAID' && (
                  <button
                    className="rounded-md px-2 py-1 text-sm font-medium text-teal-700 hover:bg-teal-50"
                    onClick={() => pay(invoice)}
                  >
                    Mark paid
                  </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </section>
  )
}
