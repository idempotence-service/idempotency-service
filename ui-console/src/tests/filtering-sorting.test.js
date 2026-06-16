import { describe, it, expect } from 'vitest'

describe('Filtering and Sorting Logic', () => {
  describe('multi-criteria filtering', () => {
    it('filters by multiple criteria - all match', () => {
      const items = [
        { type: 'A', status: 'active', value: 10 },
        { type: 'B', status: 'active', value: 20 },
        { type: 'A', status: 'inactive', value: 30 },
      ]
      const filtered = items.filter(item => 
        item.type === 'A' && item.status === 'active' && item.value > 5
      )
      expect(filtered).toHaveLength(1)
      expect(filtered[0].value).toBe(10)
    })

    it('filters by multiple criteria - partial match', () => {
      const items = [
        { type: 'A', status: 'active', value: 10 },
        { type: 'A', status: 'active', value: 20 },
        { type: 'B', status: 'active', value: 30 },
      ]
      const filtered = items.filter(item => 
        item.type === 'A' && item.status === 'active'
      )
      expect(filtered).toHaveLength(2)
    })

    it('filters by multiple criteria - no match', () => {
      const items = [
        { type: 'A', status: 'active', value: 10 },
        { type: 'B', status: 'active', value: 20 },
      ]
      const filtered = items.filter(item => 
        item.type === 'C' && item.status === 'inactive'
      )
      expect(filtered).toHaveLength(0)
    })
  })

  describe('sorting with multiple fields', () => {
    it('sorts by primary field', () => {
      const items = [
        { id: 3, name: 'C' },
        { id: 1, name: 'A' },
        { id: 2, name: 'B' },
      ]
      const sorted = [...items].sort((a, b) => a.id - b.id)
      expect(sorted[0].id).toBe(1)
      expect(sorted[1].id).toBe(2)
      expect(sorted[2].id).toBe(3)
    })

    it('sorts by secondary field when primary is equal', () => {
      const items = [
        { id: 1, name: 'B' },
        { id: 1, name: 'A' },
        { id: 2, name: 'C' },
      ]
      const sorted = [...items].sort((a, b) => {
        if (a.id !== b.id) return a.id - b.id
        return a.name.localeCompare(b.name)
      })
      expect(sorted[0].name).toBe('A')
      expect(sorted[1].name).toBe('B')
      expect(sorted[2].name).toBe('C')
    })

    it('sorts in descending order', () => {
      const items = [
        { value: 10 },
        { value: 30 },
        { value: 20 },
      ]
      const sorted = [...items].sort((a, b) => b.value - a.value)
      expect(sorted[0].value).toBe(30)
      expect(sorted[1].value).toBe(20)
      expect(sorted[2].value).toBe(10)
    })
  })

  describe('search with fuzzy matching', () => {
    it('matches exact search term', () => {
      const items = ['apple', 'banana', 'cherry']
      const searchTerm = 'apple'
      const matches = items.filter(item => 
        item.toLowerCase().includes(searchTerm.toLowerCase())
      )
      expect(matches).toHaveLength(1)
      expect(matches[0]).toBe('apple')
    })

    it('matches partial search term', () => {
      const items = ['apple', 'pineapple', 'grape']
      const searchTerm = 'apple'
      const matches = items.filter(item => 
        item.toLowerCase().includes(searchTerm.toLowerCase())
      )
      expect(matches).toHaveLength(2)
    })

    it('handles case-insensitive search', () => {
      const items = ['Apple', 'APPLE', 'apple']
      const searchTerm = 'apple'
      const matches = items.filter(item => 
        item.toLowerCase().includes(searchTerm.toLowerCase())
      )
      expect(matches).toHaveLength(3)
    })

    it('handles empty search term', () => {
      const items = ['apple', 'banana', 'cherry']
      const searchTerm = ''
      const matches = searchTerm ? items.filter(item => 
        item.toLowerCase().includes(searchTerm.toLowerCase())
      ) : items
      expect(matches).toHaveLength(3)
    })
  })

  describe('pagination with filtering', () => {
    it('paginates filtered results', () => {
      const items = Array.from({ length: 25 }, (_, i) => ({ id: i }))
      const filtered = items.filter(item => item.id % 2 === 0)
      const page = 2
      const pageSize = 5
      const start = (page - 1) * pageSize
      const end = start + pageSize
      const pageItems = filtered.slice(start, end)
      
      expect(pageItems).toHaveLength(5)
      expect(pageItems[0].id).toBe(10)
    })

    it('handles empty filtered results', () => {
      const items = Array.from({ length: 25 }, (_, i) => ({ id: i }))
      const filtered = items.filter(item => item.id > 100)
      const page = 1
      const pageSize = 10
      const start = (page - 1) * pageSize
      const end = start + pageSize
      const pageItems = filtered.slice(start, end)
      
      expect(pageItems).toHaveLength(0)
    })

    it('handles last page with fewer items', () => {
      const items = Array.from({ length: 25 }, (_, i) => ({ id: i }))
      const filtered = items.filter(item => item.id % 2 === 0)
      const page = 3
      const pageSize = 5
      const start = (page - 1) * pageSize
      const end = start + pageSize
      const pageItems = filtered.slice(start, end)
      
      expect(pageItems).toHaveLength(3)
    })
  })

  describe('grouping and aggregation', () => {
    it('groups by field', () => {
      const items = [
        { type: 'A', value: 10 },
        { type: 'B', value: 20 },
        { type: 'A', value: 30 },
        { type: 'B', value: 40 },
      ]
      const grouped = items.reduce((acc, item) => {
        acc[item.type] = (acc[item.type] || 0) + item.value
        return acc
      }, {})
      
      expect(grouped.A).toBe(40)
      expect(grouped.B).toBe(60)
    })

    it('counts occurrences', () => {
      const items = ['A', 'B', 'A', 'C', 'A', 'B']
      const counts = items.reduce((acc, item) => {
        acc[item] = (acc[item] || 0) + 1
        return acc
      }, {})
      
      expect(counts.A).toBe(3)
      expect(counts.B).toBe(2)
      expect(counts.C).toBe(1)
    })

    it('calculates average per group', () => {
      const items = [
        { group: 'A', value: 10 },
        { group: 'A', value: 20 },
        { group: 'B', value: 30 },
        { group: 'B', value: 40 },
      ]
      const grouped = items.reduce((acc, item) => {
        if (!acc[item.group]) {
          acc[item.group] = { sum: 0, count: 0 }
        }
        acc[item.group].sum += item.value
        acc[item.group].count += 1
        return acc
      }, {})
      
      const avgA = grouped.A.sum / grouped.A.count
      const avgB = grouped.B.sum / grouped.B.count
      
      expect(avgA).toBe(15)
      expect(avgB).toBe(35)
    })
  })
})
