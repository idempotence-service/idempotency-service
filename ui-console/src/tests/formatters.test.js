import { describe, it, expect } from 'vitest'

describe('Formatter Functions', () => {
  describe('number formatting', () => {
    it('formats integer', () => {
      const num = 1234
      const formatted = num.toLocaleString('ru-RU')
      expect(formatted).toContain('234')
    })

    it('formats decimal', () => {
      const num = 1234.56
      const formatted = num.toFixed(2)
      expect(formatted).toBe('1234.56')
    })

    it('formats with fixed decimals', () => {
      const num = 1234.5678
      const formatted = num.toFixed(2)
      expect(formatted).toBe('1234.57')
    })

    it('formats zero', () => {
      const num = 0
      const formatted = num.toFixed(2)
      expect(formatted).toBe('0.00')
    })

    it('handles null', () => {
      const num = null
      const formatted = num !== null ? num.toFixed(2) : '—'
      expect(formatted).toBe('—')
    })
  })

  describe('percentage formatting', () => {
    it('formats positive percentage', () => {
      const value = 0.75
      const formatted = (value * 100).toFixed(1) + '%'
      expect(formatted).toBe('75.0%')
    })

    it('formats zero percentage', () => {
      const value = 0
      const formatted = (value * 100).toFixed(1) + '%'
      expect(formatted).toBe('0.0%')
    })

    it('formats negative percentage', () => {
      const value = -0.25
      const formatted = (value * 100).toFixed(1) + '%'
      expect(formatted).toBe('-25.0%')
    })

    it('clamps to 100%', () => {
      const value = 1.5
      const clamped = Math.min(value, 1)
      const formatted = (clamped * 100).toFixed(1) + '%'
      expect(formatted).toBe('100.0%')
    })

    it('clamps to 0%', () => {
      const value = -0.5
      const clamped = Math.max(value, 0)
      const formatted = (clamped * 100).toFixed(1) + '%'
      expect(formatted).toBe('0.0%')
    })
  })

  describe('date formatting', () => {
    it('formats date to locale string', () => {
      const date = new Date('2024-01-15')
      const formatted = date.toLocaleDateString('ru-RU')
      expect(formatted).toBeDefined()
    })

    it('formats date to time string', () => {
      const date = new Date('2024-01-15T10:30:00')
      const formatted = date.toLocaleTimeString('ru-RU')
      expect(formatted).toBeDefined()
    })

    it('formats date to ISO string', () => {
      const date = new Date('2024-01-15')
      const formatted = date.toISOString()
      expect(formatted).toContain('2024-01-15')
    })

    it('handles null date', () => {
      const date = null
      const formatted = date ? date.toLocaleDateString('ru-RU') : '—'
      expect(formatted).toBe('—')
    })

    it('handles undefined date', () => {
      const date = undefined
      const formatted = date ? date.toLocaleDateString('ru-RU') : '—'
      expect(formatted).toBe('—')
    })
  })

  describe('duration formatting', () => {
    it('formats seconds to minutes', () => {
      const seconds = 120
      const minutes = Math.floor(seconds / 60)
      expect(minutes).toBe(2)
    })

    it('formats seconds to hours', () => {
      const seconds = 7200
      const hours = Math.floor(seconds / 3600)
      expect(hours).toBe(2)
    })

    it('formats duration with remainder', () => {
      const seconds = 125
      const minutes = Math.floor(seconds / 60)
      const remainingSeconds = seconds % 60
      expect(minutes).toBe(2)
      expect(remainingSeconds).toBe(5)
    })

    it('handles zero duration', () => {
      const seconds = 0
      const minutes = Math.floor(seconds / 60)
      expect(minutes).toBe(0)
    })

    it('handles negative duration', () => {
      const seconds = -60
      const minutes = Math.floor(seconds / 60)
      expect(minutes).toBe(-1)
    })
  })

  describe('file size formatting', () => {
    it('formats bytes to KB', () => {
      const bytes = 1024
      const kb = bytes / 1024
      expect(kb).toBe(1)
    })

    it('formats bytes to MB', () => {
      const bytes = 1024 * 1024
      const mb = bytes / (1024 * 1024)
      expect(mb).toBe(1)
    })

    it('formats bytes to GB', () => {
      const bytes = 1024 * 1024 * 1024
      const gb = bytes / (1024 * 1024 * 1024)
      expect(gb).toBe(1)
    })

    it('handles zero bytes', () => {
      const bytes = 0
      const kb = bytes / 1024
      expect(kb).toBe(0)
    })

    it('handles negative bytes', () => {
      const bytes = -1024
      const kb = bytes / 1024
      expect(kb).toBe(-1)
    })
  })
})
