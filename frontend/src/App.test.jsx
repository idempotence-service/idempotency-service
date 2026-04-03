import React from 'react'
import { render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from './App'

afterEach(() => {
  vi.restoreAllMocks()
})

describe('App', () => {
  it('renders title and backend status', async () => {
    vi.stubGlobal('fetch', vi.fn(() =>
      Promise.resolve({
        ok: true,
        json: () => Promise.resolve({
          status: 'ok',
          app: 'demo-backend',
          time: 'now',
          kafkaBootstrapServers: 'kafka:9092'
        })
      })
    ))

    render(<App />)

    expect(screen.getByText('React frontend is alive')).toBeInTheDocument()
    expect(await screen.findByText(/demo-backend/i)).toBeInTheDocument()
  })
})