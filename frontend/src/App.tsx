import { useEffect, useState } from 'react'
import { api, tokenStore } from './api'
import LoginPage from './components/LoginPage'
import PatientsPage from './components/PatientsPage'

type Session = 'checking' | 'signed-out' | 'signed-in'

export default function App() {
  const [session, setSession] = useState<Session>(tokenStore.get() ? 'checking' : 'signed-out')

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
  }

  if (session === 'checking') {
    return <div className="grid min-h-screen place-items-center text-slate-500">Loading…</div>
  }
  if (session === 'signed-out') {
    return <LoginPage onSignedIn={() => setSession('signed-in')} />
  }
  return <PatientsPage onSignOut={signOut} />
}
