import { describe, it, expect } from 'vitest'
import { formatDate, formatJson, canRetry } from '../utils/eventDetailHelpers.js'

describe('eventDetailHelpers', () => {
  describe('formatDate', () => {
    it('returns dash for null', () => {
      expect(formatDate(null)).toBe('—')
    })

    it('returns dash for undefined', () => {
      expect(formatDate(undefined)).toBe('—')
    })

    it('returns dash for empty string', () => {
      expect(formatDate('')).toBe('—')
    })

    it('formats valid date string', () => {
      const result = formatDate('2024-01-15T12:30:45Z')
      expect(result).toBeDefined()
      expect(result).not.toBe('—')
      expect(typeof result).toBe('string')
    })

    it('formats valid date object', () => {
      const result = formatDate(new Date('2024-01-15T12:30:45Z'))
      expect(result).toBeDefined()
      expect(result).not.toBe('—')
    })
  })

  describe('formatJson', () => {
    it('formats object with indentation', () => {
      const obj = { key: 'value', nested: { prop: 123 } }
      const result = formatJson(obj)
      expect(result).toContain('  ')
      expect(result).toContain('key')
      expect(result).toContain('value')
    })

    it('formats array with indentation', () => {
      const arr = [1, 2, 3]
      const result = formatJson(arr)
      expect(result).toContain('  ')
      expect(result).toContain('1')
    })

    it('formats null', () => {
      const result = formatJson(null)
      expect(result).toBe('null')
    })

    it('formats string', () => {
      const result = formatJson('test')
      expect(result).toBe('"test"')
    })

    it('formats number', () => {
      const result = formatJson(42)
      expect(result).toBe('42')
    })
  })

  describe('canRetry', () => {
    it('returns true for ERROR status', () => {
      expect(canRetry('ERROR')).toBe(true)
    })

    it('returns true for FAILED status', () => {
      expect(canRetry('FAILED')).toBe(true)
    })

    it('returns false for SUCCESS status', () => {
      expect(canRetry('SUCCESS')).toBe(false)
    })

    it('returns false for PROCESSING status', () => {
      expect(canRetry('PROCESSING')).toBe(false)
    })

    it('returns false for null status', () => {
      expect(canRetry(null)).toBe(false)
    })

    it('returns false for undefined status', () => {
      expect(canRetry(undefined)).toBe(false)
    })

    it('returns false for empty string', () => {
      expect(canRetry('')).toBe(false)
    })
  })
})
