import { describe, it, expect } from 'vitest'

describe('Data Transformation Functions', () => {
  describe('array transformations', () => {
    it('maps array to new format', () => {
      const items = [{ id: 1, value: 'A' }, { id: 2, value: 'B' }]
      const mapped = items.map(item => ({ key: item.id, label: item.value }))
      expect(mapped).toEqual([{ key: 1, label: 'A' }, { key: 2, label: 'B' }])
    })

    it('filters array by condition', () => {
      const items = [{ id: 1, active: true }, { id: 2, active: false }]
      const filtered = items.filter(item => item.active)
      expect(filtered).toHaveLength(1)
    })

    it('reduces array to single value', () => {
      const items = [1, 2, 3, 4]
      const sum = items.reduce((acc, item) => acc + item, 0)
      expect(sum).toBe(10)
    })

    it('chains transformations', () => {
      const items = [1, 2, 3, 4, 5]
      const result = items
        .filter(x => x > 2)
        .map(x => x * 2)
        .reduce((acc, x) => acc + x, 0)
      expect(result).toBe(24)
    })
  })

  describe('object transformations', () => {
    it('transforms object keys', () => {
      const obj = { firstName: 'John', lastName: 'Doe' }
      const transformed = Object.fromEntries(
        Object.entries(obj).map(([key, value]) => [key.toUpperCase(), value])
      )
      expect(transformed).toEqual({ FIRSTNAME: 'John', LASTNAME: 'Doe' })
    })

    it('merges objects', () => {
      const obj1 = { a: 1, b: 2 }
      const obj2 = { c: 3, d: 4 }
      const merged = { ...obj1, ...obj2 }
      expect(merged).toEqual({ a: 1, b: 2, c: 3, d: 4 })
    })

    it('picks specific keys', () => {
      const obj = { a: 1, b: 2, c: 3, d: 4 }
      const keys = ['a', 'c']
      const picked = keys.reduce((acc, key) => {
        if (key in obj) acc[key] = obj[key]
        return acc
      }, {})
      expect(picked).toEqual({ a: 1, c: 3 })
    })

    it('omits specific keys', () => {
      const obj = { a: 1, b: 2, c: 3, d: 4 }
      const keys = ['b', 'd']
      const omitted = Object.keys(obj).reduce((acc, key) => {
        if (!keys.includes(key)) acc[key] = obj[key]
        return acc
      }, {})
      expect(omitted).toEqual({ a: 1, c: 3 })
    })
  })

  describe('string transformations', () => {
    it('converts to uppercase', () => {
      const str = 'hello'
      const upper = str.toUpperCase()
      expect(upper).toBe('HELLO')
    })

    it('converts to lowercase', () => {
      const str = 'HELLO'
      const lower = str.toLowerCase()
      expect(lower).toBe('hello')
    })

    it('capitalizes first letter', () => {
      const str = 'hello'
      const capitalized = str.charAt(0).toUpperCase() + str.slice(1)
      expect(capitalized).toBe('Hello')
    })

    it('removes whitespace', () => {
      const str = '  hello  world  '
      const trimmed = str.trim()
      expect(trimmed).toBe('hello  world')
    })

    it('splits string', () => {
      const str = 'a,b,c'
      const parts = str.split(',')
      expect(parts).toEqual(['a', 'b', 'c'])
    })

    it('joins array', () => {
      const arr = ['a', 'b', 'c']
      const joined = arr.join(',')
      expect(joined).toBe('a,b,c')
    })
  })

  describe('conditional transformations', () => {
    it('applies transformation when condition is true', () => {
      const value = 10
      const condition = true
      const result = condition ? value * 2 : value
      expect(result).toBe(20)
    })

    it('skips transformation when condition is false', () => {
      const value = 10
      const condition = false
      const result = condition ? value * 2 : value
      expect(result).toBe(10)
    })

    it('applies different transformations based on condition', () => {
      const value = 10
      const condition = true
      const result = condition ? value * 2 : value / 2
      expect(result).toBe(20)
    })

    it('handles null value in transformation', () => {
      const value = null
      const result = value !== null ? value * 2 : 0
      expect(result).toBe(0)
    })
  })

  describe('nested transformations', () => {
    it('transforms nested array', () => {
      const nested = [[1, 2], [3, 4], [5, 6]]
      const flattened = nested.flat()
      const doubled = flattened.map(x => x * 2)
      expect(doubled).toEqual([2, 4, 6, 8, 10, 12])
    })

    it('transforms nested object', () => {
      const nested = { data: { values: [1, 2, 3] } }
      const sum = nested.data.values.reduce((acc, x) => acc + x, 0)
      expect(sum).toBe(6)
    })

    it('handles missing nested property', () => {
      const nested = { data: null }
      const values = nested.data?.values || []
      expect(values).toEqual([])
    })
  })
})
