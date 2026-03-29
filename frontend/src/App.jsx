import React from 'react'
import { useEffect, useState } from 'react'

export default function App() {
  const [loading, setLoading] = useState(true)
  const [data, setData] = useState(null)
  const [error, setError] = useState('')

  useEffect(() => {
    fetch('/api/v1/ping')
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`HTTP ${response.status}`)
        }
        return response.json()
      })
      .then((payload) => {
        setData(payload)
        setLoading(false)
      })
      .catch((err) => {
        setError(err.message)
        setLoading(false)
      })
  }, [])

  return (
    <main className="page">
      <section className="card">
        <p className="eyebrow">VPS + GitHub deployment starter</p>
        <h1>React frontend is alive</h1>
        <p className="muted">
          Эта страница проверяет backend через <code>/api/v1/ping</code>.
        </p>

        {loading && <p>Проверяем backend...</p>}

        {error && (
          <div className="status error">
            <strong>Ошибка:</strong> {error}
          </div>
        )}

        {data && (
          <div className="status ok">
            <div><strong>Status:</strong> {data.status}</div>
            <div><strong>App:</strong> {data.app}</div>
            <div><strong>Time:</strong> {data.time}</div>
            <div><strong>Kafka:</strong> {data.kafkaBootstrapServers}</div>
          </div>
        )}
      </section>
    </main>
  )
}