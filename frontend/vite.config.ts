import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// In development, forward API calls to the gateway so the browser stays same-origin.
const gateway = 'http://localhost:4004'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 3000,
    proxy: {
      '/auth': gateway,
      '/api': gateway,
    },
  },
})
