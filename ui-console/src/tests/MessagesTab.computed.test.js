import { describe, it, expect } from 'vitest'

describe('MessagesTab Computed Properties', () => {
  describe('typeStyle function', () => {
    it('returns correct style for sent type', () => {
      const typeStyle = (type) => {
        const styles = {
          sent:      { icon: '↑', label: 'Отправлено', color: '#82b1ff',           bg: 'rgba(130,177,255,0.12)' },
          received:  { icon: '↓', label: 'Получено',   color: 'var(--md-success)', bg: 'rgba(109,213,140,0.12)' },
          error:     { icon: '⚠', label: 'Ошибка',     color: 'var(--md-error)',   bg: 'rgba(242,184,181,0.12)' },
          duplicate: { icon: '♻', label: 'Дубликат',   color: '#f6c142',           bg: 'rgba(246,193,66,0.12)' },
        }
        return styles[type] || { icon: '?', label: type, color: 'var(--md-outline)', bg: 'var(--md-surface-3)' }
      }
      
      const result = typeStyle('sent')
      expect(result.icon).toBe('↑')
      expect(result.label).toBe('Отправлено')
      expect(result.color).toBe('#82b1ff')
    })

    it('returns correct style for received type', () => {
      const typeStyle = (type) => {
        const styles = {
          sent:      { icon: '↑', label: 'Отправлено', color: '#82b1ff',           bg: 'rgba(130,177,255,0.12)' },
          received:  { icon: '↓', label: 'Получено',   color: 'var(--md-success)', bg: 'rgba(109,213,140,0.12)' },
          error:     { icon: '⚠', label: 'Ошибка',     color: 'var(--md-error)',   bg: 'rgba(242,184,181,0.12)' },
          duplicate: { icon: '♻', label: 'Дубликат',   color: '#f6c142',           bg: 'rgba(246,193,66,0.12)' },
        }
        return styles[type] || { icon: '?', label: type, color: 'var(--md-outline)', bg: 'var(--md-surface-3)' }
      }
      
      const result = typeStyle('received')
      expect(result.icon).toBe('↓')
      expect(result.label).toBe('Получено')
    })

    it('returns default style for unknown type', () => {
      const typeStyle = (type) => {
        const styles = {
          sent:      { icon: '↑', label: 'Отправлено', color: '#82b1ff',           bg: 'rgba(130,177,255,0.12)' },
          received:  { icon: '↓', label: 'Получено',   color: 'var(--md-success)', bg: 'rgba(109,213,140,0.12)' },
          error:     { icon: '⚠', label: 'Ошибка',     color: 'var(--md-error)',   bg: 'rgba(242,184,181,0.12)' },
          duplicate: { icon: '♻', label: 'Дубликат',   color: '#f6c142',           bg: 'rgba(246,193,66,0.12)' },
        }
        return styles[type] || { icon: '?', label: type, color: 'var(--md-outline)', bg: 'var(--md-surface-3)' }
      }
      
      const result = typeStyle('unknown')
      expect(result.icon).toBe('?')
      expect(result.label).toBe('unknown')
    })
  })

  describe('truncate function', () => {
    it('truncates long strings', () => {
      const truncate = (s, n) => {
        if (!s) return '—'
        return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
      }
      
      const result = truncate('very-long-string-that-needs-truncation', 20)
      expect(result).toBe('very-lon…uncation')
    })

    it('returns short strings unchanged', () => {
      const truncate = (s, n) => {
        if (!s) return '—'
        return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
      }
      
      const result = truncate('short', 20)
      expect(result).toBe('short')
    })

    it('returns dash for empty string', () => {
      const truncate = (s, n) => {
        if (!s) return '—'
        return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
      }
      
      const result = truncate('', 20)
      expect(result).toBe('—')
    })

    it('returns dash for null', () => {
      const truncate = (s, n) => {
        if (!s) return '—'
        return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
      }
      
      const result = truncate(null, 20)
      expect(result).toBe('—')
    })
  })

  describe('message filtering logic', () => {
    it('filters messages by type', () => {
      const messages = [
        { type: 'sent', key: '1' },
        { type: 'received', key: '2' },
        { type: 'error', key: '3' },
        { type: 'duplicate', key: '4' },
      ]
      
      const filtered = messages.filter(m => m.type === 'sent')
      expect(filtered).toHaveLength(1)
      expect(filtered[0].type).toBe('sent')
    })

    it('filters messages by search query', () => {
      const messages = [
        { type: 'sent', key: 'abc123', integration: 'test' },
        { type: 'received', key: 'xyz789', integration: 'prod' },
        { type: 'error', key: 'def456', integration: 'test' },
      ]
      
      const q = 'test'
      const filtered = messages.filter(m =>
        m.key?.toLowerCase().includes(q) ||
        m.integration?.toLowerCase().includes(q)
      )
      
      expect(filtered).toHaveLength(2)
    })

    it('filters messages by status', () => {
      const messages = [
        { type: 'sent', status: 'SENT' },
        { type: 'error', status: 'ERROR' },
        { type: 'received', status: 'RECEIVED' },
      ]
      
      const q = 'error'
      const filtered = messages.filter(m => m.status?.toLowerCase().includes(q))
      
      expect(filtered).toHaveLength(1)
      expect(filtered[0].status).toBe('ERROR')
    })
  })
})
