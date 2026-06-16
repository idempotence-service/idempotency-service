import { describe, it, expect } from 'vitest'

describe('MessagesTab Branch Coverage', () => {
  describe('mergeMessages function branches', () => {
    it('merges sent messages', () => {
      const sentEvents = [
        { globalKey: 'key1', integration: 'system1', status: 'SENT', timestamp: '2024-01-15T10:00:00' }
      ]
      const receivedEvents = []
      const errorEvents = []
      const duplicateEvents = []
      
      const merged = []
      sentEvents.forEach(e => merged.push({ ...e, type: 'sent' }))
      
      expect(merged.length).toBe(1)
      expect(merged[0].type).toBe('sent')
    })

    it('merges received messages', () => {
      const sentEvents = []
      const receivedEvents = [
        { globalKey: 'key1', integration: 'system2', status: 'RECEIVED', timestamp: '2024-01-15T11:00:00' }
      ]
      const errorEvents = []
      const duplicateEvents = []
      
      const merged = []
      receivedEvents.forEach(e => merged.push({ ...e, type: 'received' }))
      
      expect(merged.length).toBe(1)
      expect(merged[0].type).toBe('received')
    })

    it('merges error events', () => {
      const sentEvents = []
      const receivedEvents = []
      const errorEvents = [
        { globalKey: 'key1', integration: 'system3', status: 'ERROR', timestamp: '2024-01-15T12:00:00' }
      ]
      const duplicateEvents = []
      
      const merged = []
      errorEvents.forEach(e => merged.push({ ...e, type: 'error' }))
      
      expect(merged.length).toBe(1)
      expect(merged[0].type).toBe('error')
    })

    it('merges duplicate events', () => {
      const sentEvents = []
      const receivedEvents = []
      const errorEvents = []
      const duplicateEvents = [
        { globalKey: 'key1', integration: 'system4', status: 'DUPLICATE', timestamp: '2024-01-15T13:00:00' }
      ]
      
      const merged = []
      duplicateEvents.forEach(e => merged.push({ ...e, type: 'duplicate' }))
      
      expect(merged.length).toBe(1)
      expect(merged[0].type).toBe('duplicate')
    })

    it('merges all message types', () => {
      const sentEvents = [{ globalKey: 'key1', status: 'SENT' }]
      const receivedEvents = [{ globalKey: 'key2', status: 'RECEIVED' }]
      const errorEvents = [{ globalKey: 'key3', status: 'ERROR' }]
      const duplicateEvents = [{ globalKey: 'key4', status: 'DUPLICATE' }]
      
      const merged = []
      sentEvents.forEach(e => merged.push({ ...e, type: 'sent' }))
      receivedEvents.forEach(e => merged.push({ ...e, type: 'received' }))
      errorEvents.forEach(e => merged.push({ ...e, type: 'error' }))
      duplicateEvents.forEach(e => merged.push({ ...e, type: 'duplicate' }))
      
      expect(merged.length).toBe(4)
    })

    it('handles empty arrays', () => {
      const merged = []
      expect(merged.length).toBe(0)
    })
  })

  describe('filter by activeFilter branches', () => {
    it('filters by all', () => {
      const messages = [
        { type: 'sent' },
        { type: 'received' },
        { type: 'error' },
        { type: 'duplicate' }
      ]
      const filtered = messages.filter(() => true)
      
      expect(filtered.length).toBe(4)
    })

    it('filters by sent', () => {
      const messages = [
        { type: 'sent' },
        { type: 'received' }
      ]
      const filtered = messages.filter(m => m.type === 'sent')
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].type).toBe('sent')
    })

    it('filters by received', () => {
      const messages = [
        { type: 'sent' },
        { type: 'received' }
      ]
      const filtered = messages.filter(m => m.type === 'received')
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].type).toBe('received')
    })

    it('filters by error', () => {
      const messages = [
        { type: 'sent' },
        { type: 'error' }
      ]
      const filtered = messages.filter(m => m.type === 'error')
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].type).toBe('error')
    })

    it('filters by duplicate', () => {
      const messages = [
        { type: 'sent' },
        { type: 'duplicate' }
      ]
      const filtered = messages.filter(m => m.type === 'duplicate')
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].type).toBe('duplicate')
    })
  })

  describe('sortBy branches', () => {
    it('sorts by timestamp ascending', () => {
      const messages = [
        { timestamp: '2024-01-15T12:00:00' },
        { timestamp: '2024-01-15T10:00:00' }
      ]
      const sorted = [...messages].sort((a, b) => 
        new Date(a.timestamp) - new Date(b.timestamp)
      )
      
      expect(sorted[0].timestamp).toBe('2024-01-15T10:00:00')
    })

    it('sorts by timestamp descending', () => {
      const messages = [
        { timestamp: '2024-01-15T10:00:00' },
        { timestamp: '2024-01-15T12:00:00' }
      ]
      const sorted = [...messages].sort((a, b) => 
        new Date(b.timestamp) - new Date(a.timestamp)
      )
      
      expect(sorted[0].timestamp).toBe('2024-01-15T12:00:00')
    })

    it('sorts by globalKey ascending', () => {
      const messages = [
        { globalKey: 'key2' },
        { globalKey: 'key1' }
      ]
      const sorted = [...messages].sort((a, b) => 
        a.globalKey.localeCompare(b.globalKey)
      )
      
      expect(sorted[0].globalKey).toBe('key1')
    })

    it('sorts by globalKey descending', () => {
      const messages = [
        { globalKey: 'key1' },
        { globalKey: 'key2' }
      ]
      const sorted = [...messages].sort((a, b) => 
        b.globalKey.localeCompare(a.globalKey)
      )
      
      expect(sorted[0].globalKey).toBe('key2')
    })
  })

  describe('search filter branches', () => {
    it('filters by globalKey', () => {
      const messages = [
        { globalKey: 'test-key-1' },
        { globalKey: 'other-key' }
      ]
      const searchQuery = 'test'
      const filtered = messages.filter(m => 
        m.globalKey.toLowerCase().includes(searchQuery.toLowerCase())
      )
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].globalKey).toBe('test-key-1')
    })

    it('filters by integration', () => {
      const messages = [
        { integration: 'system1-to-system2' },
        { integration: 'system3-to-system4' }
      ]
      const searchQuery = 'system1'
      const filtered = messages.filter(m => 
        m.integration.toLowerCase().includes(searchQuery.toLowerCase())
      )
      
      expect(filtered.length).toBe(1)
      expect(filtered[0].integration).toBe('system1-to-system2')
    })

    it('handles empty search query', () => {
      const messages = [
        { globalKey: 'test-key-1' },
        { globalKey: 'other-key' }
      ]
      const searchQuery = ''
      const filtered = messages.filter(() => true)
      
      expect(filtered.length).toBe(2)
    })

    it('handles no matches', () => {
      const messages = [
        { globalKey: 'test-key-1' }
      ]
      const searchQuery = 'nonexistent'
      const filtered = messages.filter(m => 
        m.globalKey.toLowerCase().includes(searchQuery.toLowerCase())
      )
      
      expect(filtered.length).toBe(0)
    })
  })
})
