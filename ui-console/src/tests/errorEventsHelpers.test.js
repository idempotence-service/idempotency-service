import { describe, it, expect } from 'vitest'
import { truncateKey, toggleSort, calculatePaginationPages, filterEvents } from '../utils/errorEventsHelpers.js'

describe('errorEventsHelpers', () => {
  describe('truncateKey', () => {
    it('returns dash for null', () => {
      expect(truncateKey(null)).toBe('—')
    })

    it('returns dash for undefined', () => {
      expect(truncateKey(undefined)).toBe('—')
    })

    it('returns dash for empty string', () => {
      expect(truncateKey('')).toBe('—')
    })

    it('truncates long key', () => {
      const result = truncateKey('very-long-key-that-needs-truncation')
      expect(result).toContain('…')
      expect(result.length).toBeLessThan(26)
      expect(result.startsWith('very-lo')).toBe(true)
      expect(result.endsWith('ation')).toBe(true)
    })

    it('returns original key if short enough', () => {
      expect(truncateKey('short')).toBe('short')
    })
  })

  describe('toggleSort', () => {
    it('toggles from desc to asc', () => {
      expect(toggleSort('desc')).toBe('asc')
    })

    it('toggles from asc to desc', () => {
      expect(toggleSort('asc')).toBe('desc')
    })
  })

  describe('calculatePaginationPages', () => {
    it('returns all pages when total <= 7', () => {
      const result = calculatePaginationPages(5, 0)
      expect(result).toEqual([0, 1, 2, 3, 4])
    })

    it('adds ellipsis at start when cur > 2', () => {
      const result = calculatePaginationPages(10, 5)
      expect(result).toContain(0)
      expect(result).toContain('...')
    })

    it('adds ellipsis at end when cur < total - 3', () => {
      const result = calculatePaginationPages(10, 5)
      expect(result).toContain('...')
      expect(result).toContain(9)
    })

    it('returns pages around current page', () => {
      const result = calculatePaginationPages(10, 5)
      expect(result).toContain(3)
      expect(result).toContain(4)
      expect(result).toContain(5)
      expect(result).toContain(6)
      expect(result).toContain(7)
    })

    it('handles edge case at start', () => {
      const result = calculatePaginationPages(10, 0)
      expect(result[0]).toBe(0)
    })

    it('handles edge case at end', () => {
      const result = calculatePaginationPages(10, 9)
      expect(result[result.length - 1]).toBe(9)
    })
  })

  describe('filterEvents', () => {
    it('returns all events when no filters applied', () => {
      const events = [
        { globalKey: 'key1', integration: 'service1' },
        { globalKey: 'key2', integration: 'service2' }
      ]
      const result = filterEvents(events, '', '')
      expect(result).toHaveLength(2)
    })

    it('filters by globalKey', () => {
      const events = [
        { globalKey: 'test-key-123', integration: 'service1' },
        { globalKey: 'other-key-456', integration: 'service2' }
      ]
      const result = filterEvents(events, 'test', '')
      expect(result).toHaveLength(1)
      expect(result[0].globalKey).toBe('test-key-123')
    })

    it('filters by integration', () => {
      const events = [
        { globalKey: 'key1', integration: 'payment-service' },
        { globalKey: 'key2', integration: 'notification-service' }
      ]
      const result = filterEvents(events, '', 'payment')
      expect(result).toHaveLength(1)
      expect(result[0].integration).toBe('payment-service')
    })

    it('filters by both key and integration', () => {
      const events = [
        { globalKey: 'test-key-123', integration: 'payment-service' },
        { globalKey: 'test-key-456', integration: 'notification-service' },
        { globalKey: 'other-key-789', integration: 'payment-service' }
      ]
      const result = filterEvents(events, 'test', 'payment')
      expect(result).toHaveLength(1)
      expect(result[0].globalKey).toBe('test-key-123')
    })

    it('handles case insensitive filtering', () => {
      const events = [
        { globalKey: 'TEST-KEY-123', integration: 'SERVICE1' }
      ]
      const result = filterEvents(events, 'test', 'service')
      expect(result).toHaveLength(1)
    })

    it('handles null globalKey', () => {
      const events = [
        { globalKey: null, integration: 'service1' },
        { globalKey: 'key2', integration: 'service1' }
      ]
      const result = filterEvents(events, 'key', '')
      expect(result).toHaveLength(1)
    })

    it('handles null integration', () => {
      const events = [
        { globalKey: 'key1', integration: null },
        { globalKey: 'key2', integration: 'service1' }
      ]
      const result = filterEvents(events, '', 'service')
      expect(result).toHaveLength(1)
    })

    it('trims whitespace from filters', () => {
      const events = [
        { globalKey: 'test-key-123', integration: 'service1' }
      ]
      const result = filterEvents(events, '  test  ', '  service  ')
      expect(result).toHaveLength(1)
    })
  })
})
