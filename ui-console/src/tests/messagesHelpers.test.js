import { describe, it, expect } from 'vitest'
import { toggleSort, typeStyle, truncate, formatTimestamp, rebuildMessages, filterAndSortMessages, formatRawFields, calculateErrorCountFromAudit, buildFilters, isInitialLoading, isRefreshing, shouldLoadMore, parseApiResponse, calculateAuditActivitySince, extractErrorMessage } from '../utils/messagesHelpers.js'

describe('messagesHelpers', () => {
  describe('toggleSort', () => {
    it('toggles direction when same column', () => {
      const sortBy = { value: 'timestamp' }
      const sortDir = { value: 'asc' }
      
      toggleSort(sortBy, sortDir, 'timestamp')
      
      expect(sortDir.value).toBe('desc')
    })

    it('sets new column and asc direction when different column', () => {
      const sortBy = { value: 'timestamp' }
      const sortDir = { value: 'desc' }
      
      toggleSort(sortBy, sortDir, 'key')
      
      expect(sortBy.value).toBe('key')
      expect(sortDir.value).toBe('asc')
    })

    it('toggles from desc to asc when same column', () => {
      const sortBy = { value: 'timestamp' }
      const sortDir = { value: 'desc' }
      
      toggleSort(sortBy, sortDir, 'timestamp')
      
      expect(sortDir.value).toBe('asc')
    })
  })

  describe('typeStyle', () => {
    it('returns sent style', () => {
      const style = typeStyle('sent')
      expect(style.icon).toBe('↑')
      expect(style.label).toBe('Отправлено')
      expect(style.color).toBe('#82b1ff')
    })

    it('returns received style', () => {
      const style = typeStyle('received')
      expect(style.icon).toBe('↓')
      expect(style.label).toBe('Получено')
      expect(style.color).toBe('var(--md-success)')
    })

    it('returns error style', () => {
      const style = typeStyle('error')
      expect(style.icon).toBe('⚠')
      expect(style.label).toBe('Ошибка')
      expect(style.color).toBe('var(--md-error)')
    })

    it('returns duplicate style', () => {
      const style = typeStyle('duplicate')
      expect(style.icon).toBe('♻')
      expect(style.label).toBe('Дубль')
      expect(style.color).toBe('#f6c142')
    })

    it('returns default style for unknown type', () => {
      const style = typeStyle('unknown')
      expect(style.icon).toBe('?')
      expect(style.label).toBe('unknown')
      expect(style.color).toBe('var(--md-on-surface-v)')
    })

    it('returns default style for null type', () => {
      const style = typeStyle(null)
      expect(style.icon).toBe('?')
      expect(style.label).toBe(null)
      expect(style.color).toBe('var(--md-on-surface-v)')
    })

    it('returns default style for undefined type', () => {
      const style = typeStyle(undefined)
      expect(style.icon).toBe('?')
      expect(style.label).toBe(undefined)
      expect(style.color).toBe('var(--md-on-surface-v)')
    })
  })

  describe('truncate', () => {
    it('returns dash for null', () => {
      expect(truncate(null, 22)).toBe('—')
    })

    it('returns dash for undefined', () => {
      expect(truncate(undefined, 22)).toBe('—')
    })

    it('returns dash for empty string', () => {
      expect(truncate('', 22)).toBe('—')
    })

    it('truncates long string', () => {
      const result = truncate('very-long-string-that-needs-truncation', 22)
      expect(result).toContain('…')
      expect(result.length).toBeLessThan(22)
      expect(result.startsWith('very')).toBe(true)
      expect(result.endsWith('cation')).toBe(true)
    })

    it('returns original string if short enough', () => {
      expect(truncate('short', 22)).toBe('short')
    })
  })

  describe('formatTimestamp', () => {
    it('returns dash for null', () => {
      expect(formatTimestamp(null)).toBe('—')
    })

    it('returns dash for undefined', () => {
      expect(formatTimestamp(undefined)).toBe('—')
    })

    it('returns dash for empty string', () => {
      expect(formatTimestamp('')).toBe('—')
    })

    it('returns dash for invalid date', () => {
      expect(formatTimestamp('invalid-date')).toBe('—')
    })

    it('formats valid timestamp', () => {
      const timestamp = '2024-01-15T12:30:45Z'
      const result = formatTimestamp(timestamp)
      expect(result).toBeDefined()
      expect(result).not.toBe('—')
    })
  })

  describe('rebuildMessages', () => {
    it('returns empty list when all inputs are empty', () => {
      const result = rebuildMessages([], [], [], [])
      expect(result).toEqual([])
    })

    it('processes sent messages', () => {
      const sentMessages = [{ uid: 'test-uid', integration: 'test', status: 'SENT', description: 'test desc', timestamp: '2024-01-15' }]
      const result = rebuildMessages(sentMessages, [], [], [])
      
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('sent')
      expect(result[0].key).toBe('test-uid')
      expect(result[0].integration).toBe('test')
      expect(result[0].status).toBe('SENT')
    })

    it('processes received events', () => {
      const receivedEvents = [{ globalKey: 'test-key', integration: 'test', result: 'SUCCESS', resultDescription: 'ok', timestamp: '2024-01-15' }]
      const result = rebuildMessages([], receivedEvents, [], [])
      
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('received')
      expect(result[0].key).toBe('test-key')
      expect(result[0].status).toBe('SUCCESS')
    })

    it('processes error events', () => {
      const errorEvents = [{ globalKey: 'err-key', integration: 'test', status: 'ERROR', statusDescription: 'failed', createDate: '2024-01-15' }]
      const result = rebuildMessages([], [], errorEvents, [])
      
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('error')
      expect(result[0].key).toBe('err-key')
      expect(result[0].status).toBe('ERROR')
    })

    it('processes duplicate events', () => {
      const duplicateEvents = [{ globalKey: 'dup-key', integration: 'test', reason: 'duplicate', createDate: '2024-01-15' }]
      const result = rebuildMessages([], [], [], duplicateEvents)
      
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('duplicate')
      expect(result[0].key).toBe('dup-key')
      expect(result[0].status).toBe('DUPLICATE')
    })

    it('combines all message types', () => {
      const sentMessages = [{ uid: 'sent-1', integration: 'test', status: 'SENT' }]
      const receivedEvents = [{ globalKey: 'recv-1', integration: 'test', result: 'SUCCESS' }]
      const errorEvents = [{ globalKey: 'err-1', integration: 'test', status: 'ERROR' }]
      const duplicateEvents = [{ globalKey: 'dup-1', integration: 'test', reason: 'dup' }]
      
      const result = rebuildMessages(sentMessages, receivedEvents, errorEvents, duplicateEvents)
      
      expect(result).toHaveLength(4)
      expect(result.map(m => m.type)).toEqual(['sent', 'received', 'error', 'duplicate'])
    })

    it('handles missing fields with defaults', () => {
      const sentMessages = [{}]
      const result = rebuildMessages(sentMessages, [], [], [])
      
      expect(result[0].key).toBe('')
      expect(result[0].integration).toBe('')
      expect(result[0].status).toBe('SENT')
      expect(result[0].description).toBe('')
      expect(result[0].timestamp).toBe(null)
    })
  })

  describe('filterAndSortMessages', () => {
    const sampleMessages = [
      { type: 'sent', key: 'key-1', integration: 'test', status: 'SENT', timestamp: '2024-01-15T10:00:00' },
      { type: 'received', key: 'key-2', integration: 'prod', status: 'SUCCESS', timestamp: '2024-01-15T11:00:00' },
      { type: 'error', key: 'key-3', integration: 'test', status: 'ERROR', timestamp: '2024-01-15T12:00:00' },
      { type: 'duplicate', key: 'key-4', integration: 'prod', status: 'DUPLICATE', timestamp: '2024-01-15T13:00:00' },
    ]

    it('returns all messages when no filters applied', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(4)
    })

    it('filters by type', () => {
      const result = filterAndSortMessages(sampleMessages, 'sent', '', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('sent')
    })

    it('filters by search key', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', 'key-1', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(1)
      expect(result[0].key).toBe('key-1')
    })

    it('filters by search integration', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', 'test', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(2)
      expect(result.every(m => m.integration === 'test')).toBe(true)
    })

    it('filters by search status', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', 'ERROR', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(1)
      expect(result[0].status).toBe('ERROR')
    })

    it('sorts by timestamp ascending', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'timestamp', 'asc', 100)
      expect(result[0].key).toBe('key-1')
      expect(result[3].key).toBe('key-4')
    })

    it('sorts by timestamp descending', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'timestamp', 'desc', 100)
      expect(result[0].key).toBe('key-4')
      expect(result[3].key).toBe('key-1')
    })

    it('sorts by key alphabetically', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'key', 'asc', 100)
      expect(result[0].key).toBe('key-1')
      expect(result[3].key).toBe('key-4')
    })

    it('sorts by key descending', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'key', 'desc', 100)
      expect(result[0].key).toBe('key-4')
      expect(result[3].key).toBe('key-1')
    })

    it('applies display limit', () => {
      const result = filterAndSortMessages(sampleMessages, 'all', '', 'timestamp', 'desc', 2)
      expect(result).toHaveLength(2)
    })

    it('combines type filter and search', () => {
      const result = filterAndSortMessages(sampleMessages, 'sent', 'key-1', 'timestamp', 'desc', 100)
      expect(result).toHaveLength(1)
      expect(result[0].type).toBe('sent')
      expect(result[0].key).toBe('key-1')
    })

    it('handles empty messages list', () => {
      const result = filterAndSortMessages([], 'all', '', 'timestamp', 'desc', 100)
      expect(result).toEqual([])
    })

    it('handles null timestamp in sorting', () => {
      const messagesWithNull = [
        { type: 'sent', key: 'key-1', timestamp: null },
        { type: 'received', key: 'key-2', timestamp: '2024-01-15T10:00:00' },
      ]
      const result = filterAndSortMessages(messagesWithNull, 'all', '', 'timestamp', 'asc', 100)
      expect(result).toHaveLength(2)
    })
  })

  describe('formatRawFields', () => {
    it('returns empty array when selectedMsg is null', () => {
      expect(formatRawFields(null)).toEqual([])
    })

    it('returns empty array when selectedMsg is undefined', () => {
      expect(formatRawFields(undefined)).toEqual([])
    })

    it('filters out keys starting with underscore', () => {
      const msg = { _id: '123', key: 'test', status: 'sent' }
      const result = formatRawFields(msg)
      expect(result).toHaveLength(2)
      expect(result.every(([k]) => !k.startsWith('_'))).toBe(true)
    })

    it('converts null values to dash', () => {
      const msg = { key: null, status: 'sent' }
      const result = formatRawFields(msg)
      expect(result).toContainEqual(['key', '—'])
    })

    it('converts undefined values to dash', () => {
      const msg = { key: undefined, status: 'sent' }
      const result = formatRawFields(msg)
      expect(result).toContainEqual(['key', '—'])
    })

    it('stringifies objects', () => {
      const msg = { metadata: { foo: 'bar' }, status: 'sent' }
      const result = formatRawFields(msg)
      const metadataEntry = result.find(([k]) => k === 'metadata')
      expect(metadataEntry[1]).toContain('foo')
    })

    it('converts other values to string', () => {
      const msg = { count: 42, status: 'sent' }
      const result = formatRawFields(msg)
      expect(result).toContainEqual(['count', '42'])
    })

    it('uses _raw property if available', () => {
      const msg = { _raw: { key: 'test', status: 'sent' }, key: 'different' }
      const result = formatRawFields(msg)
      expect(result).toContainEqual(['key', 'test'])
    })

    it('handles nested objects', () => {
      const msg = { nested: { deep: { value: 123 } }, status: 'sent' }
      const result = formatRawFields(msg)
      const nestedEntry = result.find(([k]) => k === 'nested')
      expect(nestedEntry[1]).toContain('deep')
    })
  })

  describe('calculateErrorCountFromAudit', () => {
    it('returns 0 for empty array', () => {
      expect(calculateErrorCountFromAudit([])).toBe(0)
    })

    it('returns 0 for null', () => {
      expect(calculateErrorCountFromAudit(null)).toBe(0)
    })

    it('returns 0 for undefined', () => {
      expect(calculateErrorCountFromAudit(undefined)).toBe(0)
    })

    it('sums all error types from single slot', () => {
      const auditActivity = [
        {
          'Некорректное входящее событие': 5,
          'Не найден маршрут для входящего события': 3,
          'Некорректный ответ от системы-получателя': 2,
          'Получен ответ без ожидающей операции': 1,
          'Не получен асинхронный ответ от системы-получателя вовремя': 4
        }
      ]
      expect(calculateErrorCountFromAudit(auditActivity)).toBe(15)
    })

    it('sums across multiple slots', () => {
      const auditActivity = [
        { 'Некорректное входящее событие': 5 },
        { 'Не найден маршрут для входящего события': 3 }
      ]
      expect(calculateErrorCountFromAudit(auditActivity)).toBe(8)
    })

    it('handles missing error types', () => {
      const auditActivity = [
        { 'Некорректное входящее событие': 5 }
      ]
      expect(calculateErrorCountFromAudit(auditActivity)).toBe(5)
    })

    it('handles zero values', () => {
      const auditActivity = [
        { 'Некорректное входящее событие': 0, 'Не найден маршрут для входящего события': 0 }
      ]
      expect(calculateErrorCountFromAudit(auditActivity)).toBe(0)
    })
  })

  describe('buildFilters', () => {
    it('returns all filter options', () => {
      const auditActivity = []
      const senderStats = { totalSent: 10 }
      const receiverStats = { totalReceived: 5 }
      const totalDuplicateCount = 2

      const result = buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount)
      expect(result).toHaveLength(5)
      expect(result[0].id).toBe('all')
      expect(result[1].id).toBe('sent')
      expect(result[2].id).toBe('received')
      expect(result[3].id).toBe('error')
      expect(result[4].id).toBe('duplicate')
    })

    it('includes error count from audit activity', () => {
      const auditActivity = [{ 'Некорректное входящее событие': 5 }]
      const senderStats = { totalSent: 10 }
      const receiverStats = { totalReceived: 5 }
      const totalDuplicateCount = 2

      const result = buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount)
      expect(result[3].count).toBe(5)
    })

    it('includes sender stats', () => {
      const auditActivity = []
      const senderStats = { totalSent: 15 }
      const receiverStats = { totalReceived: 5 }
      const totalDuplicateCount = 2

      const result = buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount)
      expect(result[1].count).toBe(15)
    })

    it('includes receiver stats', () => {
      const auditActivity = []
      const senderStats = { totalSent: 10 }
      const receiverStats = { totalReceived: 20 }
      const totalDuplicateCount = 2

      const result = buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount)
      expect(result[2].count).toBe(20)
    })

    it('includes duplicate count', () => {
      const auditActivity = []
      const senderStats = { totalSent: 10 }
      const receiverStats = { totalReceived: 5 }
      const totalDuplicateCount = 7

      const result = buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount)
      expect(result[4].count).toBe(7)
    })
  })

  describe('isInitialLoading', () => {
    it('returns true when loading and no messages', () => {
      expect(isInitialLoading(true, 0)).toBe(true)
    })

    it('returns false when not loading', () => {
      expect(isInitialLoading(false, 0)).toBe(false)
    })

    it('returns false when loading but has messages', () => {
      expect(isInitialLoading(true, 10)).toBe(false)
    })

    it('returns false when not loading and has messages', () => {
      expect(isInitialLoading(false, 10)).toBe(false)
    })
  })

  describe('isRefreshing', () => {
    it('returns true when loading and has messages', () => {
      expect(isRefreshing(true, 10)).toBe(true)
    })

    it('returns false when not loading', () => {
      expect(isRefreshing(false, 10)).toBe(false)
    })

    it('returns false when loading but no messages', () => {
      expect(isRefreshing(true, 0)).toBe(false)
    })

    it('returns false when not loading and no messages', () => {
      expect(isRefreshing(false, 0)).toBe(false)
    })
  })

  describe('shouldLoadMore', () => {
    it('returns false when scrollContainer is null', () => {
      expect(shouldLoadMore(null, false, 10, 100)).toBe(false)
    })

    it('returns false when loadingMore is true', () => {
      const container = { scrollHeight: 1000, scrollTop: 500, clientHeight: 300 }
      expect(shouldLoadMore(container, true, 10, 100)).toBe(false)
    })

    it('returns false when displayLimit equals allMessagesLength', () => {
      const container = { scrollHeight: 1000, scrollTop: 500, clientHeight: 300 }
      expect(shouldLoadMore(container, false, 100, 100)).toBe(false)
    })

    it('returns false when not near bottom', () => {
      const container = { scrollHeight: 1000, scrollTop: 200, clientHeight: 300 }
      expect(shouldLoadMore(container, false, 10, 100)).toBe(false)
    })

    it('returns true when near bottom and more messages available', () => {
      const container = { scrollHeight: 1000, scrollTop: 650, clientHeight: 300 }
      expect(shouldLoadMore(container, false, 10, 100)).toBe(true)
    })

    it('uses custom threshold', () => {
      const container = { scrollHeight: 1000, scrollTop: 550, clientHeight: 300 }
      expect(shouldLoadMore(container, false, 10, 100, 200)).toBe(true)
    })

    it('calculates scrollBottom correctly', () => {
      const container = { scrollHeight: 1000, scrollTop: 750, clientHeight: 200 }
      expect(shouldLoadMore(container, false, 10, 100)).toBe(true)
    })
  })

  describe('parseApiResponse', () => {
    it('returns array when data is already array', () => {
      const data = [{ id: 1 }, { id: 2 }]
      const result = parseApiResponse(data)
      expect(result).toEqual(data)
    })

    it('wraps single object in array', () => {
      const data = { id: 1 }
      const result = parseApiResponse(data)
      expect(result).toEqual([data])
    })

    it('returns empty array when data is null', () => {
      const result = parseApiResponse(null)
      expect(result).toEqual([])
    })

    it('returns empty array when data is undefined', () => {
      const result = parseApiResponse(undefined)
      expect(result).toEqual([])
    })
  })

  describe('calculateAuditActivitySince', () => {
    it('returns timestamp 1 hour ago', () => {
      const result = calculateAuditActivitySince()
      const now = new Date()
      const oneHourAgo = new Date(now.getTime() - 60 * 60000)
      const diff = Math.abs(new Date(result).getTime() - oneHourAgo.getTime())
      expect(diff).toBeLessThan(1000) // Allow 1 second tolerance
    })

    it('returns valid ISO string', () => {
      const result = calculateAuditActivitySince()
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
    })
  })

  describe('extractErrorMessage', () => {
    it('extracts error text from response data', () => {
      const error = {
        response: {
          data: {
            error: {
              text: 'Custom error message'
            }
          }
        }
      }
      const result = extractErrorMessage(error)
      expect(result).toBe('Custom error message')
    })

    it('falls back to error message', () => {
      const error = {
        message: 'Standard error message'
      }
      const result = extractErrorMessage(error)
      expect(result).toBe('Standard error message')
    })

    it('returns default when no error info available', () => {
      const error = {}
      const result = extractErrorMessage(error)
      expect(result).toBe('неизвестно')
    })

    it('returns default for null', () => {
      const result = extractErrorMessage(null)
      expect(result).toBe('неизвестно')
    })

    it('returns default for undefined', () => {
      const result = extractErrorMessage(undefined)
      expect(result).toBe('неизвестно')
    })
  })
})
