import { describe, it, expect } from 'vitest'
import { formatDuration } from '../utils/configHelpers.js'

describe('configHelpers', () => {
  describe('formatDuration', () => {
    it('returns empty string for null', () => {
      expect(formatDuration(null)).toBe('')
    })

    it('returns empty string for undefined', () => {
      expect(formatDuration(undefined)).toBe('')
    })

    it('returns empty string for 0', () => {
      expect(formatDuration(0)).toBe('')
    })

    it('formats seconds only', () => {
      expect(formatDuration(45)).toBe('45с')
    })

    it('formats minutes and seconds', () => {
      expect(formatDuration(125)).toBe('2м 5с')
    })

    it('formats hours, minutes and seconds', () => {
      expect(formatDuration(3665)).toBe('1ч 1м 5с')
    })

    it('formats days, hours, minutes and seconds', () => {
      expect(formatDuration(90061)).toBe('1д 1ч 1м 1с')
    })

    it('formats exactly one day', () => {
      expect(formatDuration(86400)).toBe('1д')
    })

    it('formats exactly one hour', () => {
      expect(formatDuration(3600)).toBe('1ч')
    })

    it('formats exactly one minute', () => {
      expect(formatDuration(60)).toBe('1м')
    })

    it('handles large values', () => {
      expect(formatDuration(172800)).toBe('2д')
    })
  })
})
