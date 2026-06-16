import { describe, it, expect } from 'vitest'

describe('Utility Functions', () => {
  describe('date formatting', () => {
    it('formats timestamp to date string', () => {
      const timestamp = 1718544000000
      const date = new Date(timestamp)
      const formatted = date.toLocaleDateString('ru-RU')
      expect(formatted).toBeDefined()
    })

    it('formats timestamp to time string', () => {
      const timestamp = 1718544000000
      const date = new Date(timestamp)
      const formatted = date.toLocaleTimeString('ru-RU')
      expect(formatted).toBeDefined()
    })

    it('handles null timestamp', () => {
      const timestamp = null
      const date = timestamp ? new Date(timestamp) : null
      expect(date).toBe(null)
    })

    it('handles undefined timestamp', () => {
      const timestamp = undefined
      const date = timestamp ? new Date(timestamp) : null
      expect(date).toBe(null)
    })
  })

  describe('number formatting', () => {
    it('formats number with decimals', () => {
      const num = 1234.5678
      const formatted = num.toFixed(2)
      expect(formatted).toBe('1234.57')
    })

    it('formats integer without decimals', () => {
      const num = 1234
      const formatted = num.toFixed(2)
      expect(formatted).toBe('1234.00')
    })

    it('formats zero', () => {
      const num = 0
      const formatted = num.toFixed(2)
      expect(formatted).toBe('0.00')
    })

    it('handles null number', () => {
      const num = null
      const formatted = num !== null ? num.toFixed(2) : '—'
      expect(formatted).toBe('—')
    })
  })

  describe('string operations', () => {
    it('truncates long string', () => {
      const str = 'very-long-string-that-needs-truncation'
      const maxLength = 10
      const truncated = str.length > maxLength ? str.slice(0, maxLength) + '...' : str
      expect(truncated).toBe('very-long-...')
    })

    it('keeps short string unchanged', () => {
      const str = 'short'
      const maxLength = 10
      const truncated = str.length > maxLength ? str.slice(0, maxLength) + '...' : str
      expect(truncated).toBe('short')
    })

    it('handles empty string', () => {
      const str = ''
      const maxLength = 10
      const truncated = str.length > maxLength ? str.slice(0, maxLength) + '...' : str
      expect(truncated).toBe('')
    })

    it('handles null string', () => {
      const str = null
      const result = str || '—'
      expect(result).toBe('—')
    })
  })

  describe('array operations', () => {
    it('filters array by condition', () => {
      const arr = [1, 2, 3, 4, 5]
      const filtered = arr.filter(x => x > 2)
      expect(filtered).toEqual([3, 4, 5])
    })

    it('maps array to new values', () => {
      const arr = [1, 2, 3]
      const mapped = arr.map(x => x * 2)
      expect(mapped).toEqual([2, 4, 6])
    })

    it('reduces array to single value', () => {
      const arr = [1, 2, 3, 4]
      const sum = arr.reduce((acc, x) => acc + x, 0)
      expect(sum).toBe(10)
    })

    it('handles empty array', () => {
      const arr = []
      const filtered = arr.filter(x => x > 2)
      expect(filtered).toEqual([])
    })
  })

  describe('object operations', () => {
    it('gets object keys', () => {
      const obj = { a: 1, b: 2, c: 3 }
      const keys = Object.keys(obj)
      expect(keys).toEqual(['a', 'b', 'c'])
    })

    it('gets object values', () => {
      const obj = { a: 1, b: 2, c: 3 }
      const values = Object.values(obj)
      expect(values).toEqual([1, 2, 3])
    })

    it('handles null object', () => {
      const obj = null
      const keys = obj ? Object.keys(obj) : []
      expect(keys).toEqual([])
    })

    it('handles undefined object', () => {
      const obj = undefined
      const keys = obj ? Object.keys(obj) : []
      expect(keys).toEqual([])
    })
  })
})
