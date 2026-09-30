import { useEffect, useState } from 'react'
import { api, tokenStore } from './api'
import BillingPage from './components/BillingPage'
import LoginPage from './components/LoginPage'
import PatientsPage from './components/PatientsPage'
import { Button } from './components/ui'

type Session = 'checking' | 'signed-out' | 'signed-in'
type Tab = 'patients' | 'billing'

const tabs: { key: Tab; label: string }[] = [
  { key: 'patients', label: 'Patients' },
  { key: 'billing', label: 'Billing' },
]

export default function App() {
  const [session, setSession] = useState<Session>(tokenStore.get() ? 'checking' : 'signed-out')
  const [tab, setTab] = useState<Tab>('patients')

  // Confirm a stored token is still valid before showing the app.
  useEffect(() => {
    if (session !== 'checking') return
    api.validate().then((ok) => {
      if (!ok) tokenStore.clear()
      setSession(ok ? 'signed-in' : 'signed-out')
    })
  }, [session])

  const signOut = () => {
    tokenStore.clear()
    setSession('signed-out')
    setTab('patients')
  }

  if (session === 'checking') {
    return <div className="grid min-h-screen place-items-center text-slate-500">Loading…</div>
  }
  if (session === 'signed-out') {
    return <LoginPage onSignedIn={() => setSession('signed-in')} />
  }
  return (
    <div className="min-h-screen">
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3">
          <div className="flex items-center gap-2">
            <div className="grid size-8 shrink-0 place-items-center rounded-lg bg-teal-600 font-bold text-white">+</div>
            <span className="hidden font-semibold sm:inline">Patient Service</span>
          </div>
          <nav className="flex gap-1 rounded-lg bg-slate-100 p-1">
            {tabs.map((t) => (
              <button
                key={t.key}
                onClick={() => setTab(t.key)}
                aria-current={tab === t.key ? 'page' : undefined}
                className={`rounded-md px-3 py-1.5 text-sm font-medium ${
                  tab === t.key ? 'bg-white text-slate-900 shadow-sm' : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                {t.label}
              </button>
            ))}
          </nav>
          <Button variant="secondary" onClick={signOut}>
            Sign out
          </Button>
        </div>
      </header>
      {tab === 'patients' ? <PatientsPage onSignOut={signOut} /> : <BillingPage onSignOut={signOut} />}
    </div>
  )
}
