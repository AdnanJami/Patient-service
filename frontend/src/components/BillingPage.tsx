import { useCallback, useEffect, useMemo, useState } from 'react'
import { api, ApiError, type Invoice } from '../api'
import { formatDate, formatMoney, sumAmounts } from '../format'
import InvoiceStatusBadge, { isOverdue } from './InvoiceStatusBadge'
import { Alert, Toast, useNotice } from './ui'

type Filter = 'all' | 'unpaid' | 'overdue' | 'paid'

const filters: { key: Filter; label: string; match: (i: Invoice) => boolean }[] = [
  { key: 'all', label: 'All', match: () => true },
  { key: 'unpaid', label: 'Unpaid', match: (i) => i.status === 'UNPAID' },
  { key: 'overdue', label: 'Overdue', match: isOverdue },
  { key: 'paid', label: 'Paid', match: (i) => i.status === 'PAID' },
]

export default function BillingPage({ onSignOut }: { onSignOut: () => void }) {
  const [invoices, setInvoices] = useState<Invoice[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState<Filter>('all')
  const [notice, setNotice] = useNotice()

  const handleError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onSignOut()
      setError(err instanceof ApiError ? err.message : 'Could not reach the server.')
    },
    [onSignOut],
  )

  useEffect(() => {
    api
      .listInvoices()
      .then(setInvoices)
      .catch(handleError)
      .finally(() => setLoading(false))
  }, [handleError])

  const totals = useMemo(() => {
    const unpaid = invoices.filter((i) => i.status === 'UNPAID')
    const overdue = unpaid.filter(isOverdue)
    const paid = invoices.filter((i) => i.status === 'PAID')
    return [
      { label: 'Outstanding', amount: sumAmounts(unpaid.map((i) => i.amount)), count: unpaid.length, tone: 'text-slate-900' },
      { label: 'Overdue', amount: sumAmounts(overdue.map((i) => i.amount)), count: overdue.length, tone: 'text-red-600' },
      { label: 'Collected', amount: sumAmounts(paid.map((i) => i.amount)), count: paid.length, tone: 'text-emerald-700' },
    ]
  }, [invoices])

  const visible = useMemo(() => {
    const match = filters.find((f) => f.key === filter)!.match
    const q = query.trim().toLowerCase()
    return invoices.filter(
      (i) => match(i) && (!q || i.patientName.toLowerCase().includes(q) || i.description.toLowerCase().includes(q)),
    )
  }, [invoices, filter, query])

  const pay = async (invoice: Invoice) => {
    setError('')
    try {
      const paid = await api.payInvoice(invoice.id)
      setInvoices((list) => list.map((i) => (i.id === paid.id ? paid : i)))
      setNotice(`Invoice for ${paid.patientName} marked as paid`)
    } catch (err) {
      handleError(err)
    }
  }

  const payButton = (invoice: Invoice) =>
    invoice.status === 'UNPAID' ? (
      <button
        className="rounded-md px-2 py-1 text-sm font-medium text-teal-700 hover:bg-teal-50"
        onClick={() => pay(invoice)}
      >
        Mark paid
      </button>
    ) : null

  return (
    <main className="mx-auto max-w-6xl space-y-4 px-4 py-6">
      <div>
        <h1 className="text-xl font-semibold">Billing</h1>
        <p className="text-sm text-slate-500">
          {loading ? 'Loading…' : `${visible.length} of ${invoices.length} invoices shown`}
        </p>
      </div>

      <div className="grid gap-3 sm:grid-cols-3">
        {totals.map((t) => (
          <div key={t.label} className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
            <div className="text-sm text-slate-500">{t.label}</div>
            <div className={`text-2xl font-semibold tabular-nums ${t.tone}`}>{formatMoney(t.amount)}</div>
            <div className="text-xs text-slate-500">
              {t.count} invoice{t.count === 1 ? '' : 's'}
            </div>
          </div>
        ))}
      </div>

      <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
        <div className="flex gap-1 overflow-x-auto rounded-lg bg-slate-100 p-1" role="tablist">
          {filters.map((f) => (
            <button
              key={f.key}
              role="tab"
              aria-selected={filter === f.key}
              onClick={() => setFilter(f.key)}
              className={`flex-1 whitespace-nowrap rounded-md px-3 py-1.5 text-sm font-medium sm:flex-none ${
                filter === f.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              {f.label}
            </button>
          ))}
        </div>
        <input
          type="search"
          placeholder="Search patient or description"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          className="w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-teal-500 focus:outline-none focus:ring-2 focus:ring-teal-200 sm:w-72"
        />
      </div>

      {error && <Alert>{error}</Alert>}

      {!loading && visible.length === 0 && !error && (
        <div className="rounded-2xl border border-dashed border-slate-300 bg-white p-10 text-center text-sm text-slate-500">
          {invoices.length === 0
            ? 'No invoices yet. Create one from a patient’s details.'
            : 'No invoices match this filter.'}
        </div>
      )}

      {visible.length > 0 && (
        <>
          {/* Phones: cards */}
          <ul className="space-y-3 md:hidden">
            {visible.map((i) => (
              <li key={i.id} className="rounded-xl bg-white p-4 shadow-sm ring-1 ring-slate-200">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <div className="font-medium">{i.patientName}</div>
                    <div className="truncate text-sm text-slate-500">{i.description}</div>
                  </div>
                  <div className="text-right font-semibold tabular-nums">{formatMoney(i.amount)}</div>
                </div>
                <div className="mt-2 flex items-center justify-between gap-2 text-sm text-slate-500">
                  <div className="flex items-center gap-2">
                    <InvoiceStatusBadge invoice={i} />
                    <span>{i.paidDate ? `Paid ${formatDate(i.paidDate)}` : `Due ${formatDate(i.dueDate)}`}</span>
                  </div>
                  {payButton(i)}
                </div>
              </li>
            ))}
          </ul>

          {/* Tablets and up: table */}
          <div className="hidden overflow-hidden rounded-xl bg-white shadow-sm ring-1 ring-slate-200 md:block">
            <table className="w-full text-left text-sm">
              <thead className="bg-slate-50 text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-4 py-3 font-medium">Patient</th>
                  <th className="px-4 py-3 font-medium">Description</th>
                  <th className="px-4 py-3 font-medium">Issued</th>
                  <th className="px-4 py-3 font-medium">Due / paid</th>
                  <th className="px-4 py-3 font-medium">Status</th>
                  <th className="px-4 py-3 text-right font-medium">Amount</th>
                  <th className="px-4 py-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {visible.map((i) => (
                  <tr key={i.id} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium">{i.patientName}</td>
                    <td className="max-w-xs truncate px-4 py-3 text-slate-600">{i.description}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600">{formatDate(i.issuedDate)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-slate-600">
                      {i.paidDate ? `Paid ${formatDate(i.paidDate)}` : formatDate(i.dueDate)}
                    </td>
                    <td className="px-4 py-3">
                      <InvoiceStatusBadge invoice={i} />
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right font-semibold tabular-nums">
                      {formatMoney(i.amount)}
                    </td>
                    <td className="whitespace-nowrap px-4 py-3 text-right">
                      {payButton(i)}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      )}

      <Toast message={notice} />
    </main>
  )
}
