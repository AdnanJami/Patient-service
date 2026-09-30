import type { Invoice } from '../api'
import { today } from '../format'

export const isOverdue = (invoice: Invoice) => invoice.status === 'UNPAID' && invoice.dueDate < today()

export default function InvoiceStatusBadge({ invoice }: { invoice: Invoice }) {
  const [label, style] =
    invoice.status === 'PAID'
      ? ['Paid', 'bg-emerald-50 text-emerald-700 ring-emerald-200']
      : isOverdue(invoice)
        ? ['Overdue', 'bg-red-50 text-red-700 ring-red-200']
        : ['Unpaid', 'bg-amber-50 text-amber-800 ring-amber-200']
  return (
    <span className={`inline-flex items-center rounded-full px-2 py-0.5 text-xs font-medium ring-1 ring-inset ${style}`}>
      {label}
    </span>
  )
}
