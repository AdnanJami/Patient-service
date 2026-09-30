// Currency used for all amounts; change here to switch the whole app.
const CURRENCY = 'USD'

const money = new Intl.NumberFormat(undefined, { style: 'currency', currency: CURRENCY })

export const formatMoney = (amount: number) => money.format(amount)

export const formatDate = (iso: string) =>
  new Date(`${iso}T00:00:00`).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })

/** Today's date as yyyy-mm-dd in the user's time zone. */
export const today = (offsetDays = 0) => {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

/** Sums money in cents to avoid floating-point drift. */
export const sumAmounts = (amounts: number[]) =>
  amounts.reduce((cents, a) => cents + Math.round(a * 100), 0) / 100
