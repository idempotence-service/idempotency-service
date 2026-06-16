import { describe, it, expect } from 'vitest'
import { truncate, getAdaptiveTimeRange, getAdaptiveTimeRangeForData, calculateSuccessRate, formatSuccessRate, calculateThroughput, getThroughputLabel, calculateSystemHealth, buildActivityChartData, buildDuplicatesChartData, buildErrorsChartData, calculateSinceTimestamp, toggleIntegration, channelEntries } from '../utils/overviewHelpers.js'

describe('overviewHelpers', () => {
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
      expect(result).toBe('very-lon…uncation')
    })

    it('returns original string if short enough', () => {
      expect(truncate('short', 22)).toBe('short')
    })
  })

  describe('getAdaptiveTimeRange', () => {
    it('returns hourly interval for <= 24 hours', () => {
      const start = new Date('2024-01-15T00:00:00')
      const end = new Date('2024-01-15T12:00:00')
      const result = getAdaptiveTimeRange(start, end)
      
      expect(result.interval).toBe(60 * 60 * 1000)
      expect(result.format).toBeDefined()
    })

    it('returns daily interval for <= 7 days', () => {
      const start = new Date('2024-01-15T00:00:00')
      const end = new Date('2024-01-20T00:00:00')
      const result = getAdaptiveTimeRange(start, end)
      
      expect(result.interval).toBe(24 * 60 * 60 * 1000)
      expect(result.format).toBeDefined()
    })

    it('returns weekly interval for <= 30 days', () => {
      const start = new Date('2024-01-01T00:00:00')
      const end = new Date('2024-01-25T00:00:00')
      const result = getAdaptiveTimeRange(start, end)
      
      expect(result.interval).toBe(7 * 24 * 60 * 60 * 1000)
      expect(result.format).toBeDefined()
    })

    it('returns monthly interval for > 30 days', () => {
      const start = new Date('2024-01-01T00:00:00')
      const end = new Date('2024-02-15T00:00:00')
      const result = getAdaptiveTimeRange(start, end)
      
      expect(result.interval).toBe(30 * 24 * 60 * 60 * 1000)
      expect(result.format).toBeDefined()
    })
  })

  describe('getAdaptiveTimeRangeForData', () => {
    it('selects appropriate interval for small time range', () => {
      const minTime = new Date('2024-01-15T00:00:00').getTime()
      const maxTime = new Date('2024-01-15T01:00:00').getTime()
      const result = getAdaptiveTimeRangeForData(minTime, maxTime, 12)
      
      expect(result.interval).toBeDefined()
      expect(result.format).toBeDefined()
    })

    it('selects appropriate interval for large time range', () => {
      const minTime = new Date('2024-01-01T00:00:00').getTime()
      const maxTime = new Date('2024-01-15T00:00:00').getTime()
      const result = getAdaptiveTimeRangeForData(minTime, maxTime, 12)
      
      expect(result.interval).toBeDefined()
      expect(result.format).toBeDefined()
    })

    it('uses default target slots when not provided', () => {
      const minTime = new Date('2024-01-15T00:00:00').getTime()
      const maxTime = new Date('2024-01-15T01:00:00').getTime()
      const result = getAdaptiveTimeRangeForData(minTime, maxTime)
      
      expect(result.interval).toBeDefined()
      expect(result.format).toBeDefined()
    })

    it('returns time format for intervals < 1 hour', () => {
      const minTime = new Date('2024-01-15T00:00:00').getTime()
      const maxTime = new Date('2024-01-15T00:30:00').getTime()
      const result = getAdaptiveTimeRangeForData(minTime, maxTime, 12)
      
      const testDate = new Date('2024-01-15T00:15:00')
      const formatted = result.format(testDate)
      expect(formatted).toBeDefined()
    })

    it('returns date format for intervals >= 1 day', () => {
      const minTime = new Date('2024-01-01T00:00:00').getTime()
      const maxTime = new Date('2024-01-15T00:00:00').getTime()
      const result = getAdaptiveTimeRangeForData(minTime, maxTime, 12)
      
      const testDate = new Date('2024-01-08T00:00:00')
      const formatted = result.format(testDate)
      expect(formatted).toBeDefined()
    })
  })

  describe('calculateSuccessRate', () => {
    it('returns 0 when total messages is 0', () => {
      expect(calculateSuccessRate(0, 0, 0)).toBe(0)
    })

    it('calculates success rate correctly', () => {
      expect(calculateSuccessRate(100, 10, 5)).toBe(85)
    })

    it('handles only errors', () => {
      expect(calculateSuccessRate(100, 100, 0)).toBe(0)
    })

    it('handles only timeouts', () => {
      expect(calculateSuccessRate(100, 0, 100)).toBe(0)
    })

    it('handles perfect success', () => {
      expect(calculateSuccessRate(100, 0, 0)).toBe(100)
    })
  })

  describe('formatSuccessRate', () => {
    it('returns dash when total messages is 0', () => {
      expect(formatSuccessRate(0, 0, 0)).toBe('—')
    })

    it('formats success rate as percentage', () => {
      expect(formatSuccessRate(100, 10, 5)).toBe('85.0%')
    })

    it('rounds correctly', () => {
      expect(formatSuccessRate(100, 15, 3)).toBe('82.0%')
    })
  })

  describe('calculateThroughput', () => {
    it('returns sent count when time range is all', () => {
      expect(calculateThroughput(100, 'all')).toBe(100)
    })

    it('returns sent count when sent count is 0', () => {
      expect(calculateThroughput(0, 'minute')).toBe(0)
    })

    it('calculates messages per second for minute', () => {
      expect(calculateThroughput(60, 'minute')).toBe(1)
    })

    it('calculates messages per second for hour', () => {
      expect(calculateThroughput(3600, 'hour')).toBe(1)
    })

    it('returns decimal for low throughput', () => {
      expect(calculateThroughput(30, 'minute')).toBe('0.50')
    })

    it('rounds for high throughput', () => {
      expect(calculateThroughput(150, 'minute')).toBe(3)
    })
  })

  describe('getThroughputLabel', () => {
    it('returns total messages label for all time range', () => {
      expect(getThroughputLabel('all')).toBe('Всего сообщений')
    })

    it('returns per second label for other ranges', () => {
      expect(getThroughputLabel('minute')).toBe('сообщений/сек')
      expect(getThroughputLabel('hour')).toBe('сообщений/сек')
      expect(getThroughputLabel('day')).toBe('сообщений/сек')
    })
  })

  describe('calculateSystemHealth', () => {
    it('returns excellent when no errors and has messages', () => {
      const result = calculateSystemHealth(100, 0, 0)
      expect(result.status).toBe('healthy')
      expect(result.label).toBe('Отлично')
      expect(result.message).toBe('Все системы работают нормально')
    })

    it('returns healthy when error rate < 5%', () => {
      const result = calculateSystemHealth(100, 3, 0)
      expect(result.status).toBe('healthy')
      expect(result.label).toBe('Здорова')
      expect(result.message).toContain('в пределах нормы')
    })

    it('returns degraded when error rate between 5% and 15%', () => {
      const result = calculateSystemHealth(100, 10, 0)
      expect(result.status).toBe('degraded')
      expect(result.label).toBe('Деградация')
      expect(result.message).toContain('требует внимания')
    })

    it('returns critical when error rate >= 15%', () => {
      const result = calculateSystemHealth(100, 20, 0)
      expect(result.status).toBe('critical')
      expect(result.label).toBe('Критично')
      expect(result.message).toContain('требуется вмешательство')
    })

    it('handles zero messages', () => {
      const result = calculateSystemHealth(0, 0, 0)
      expect(result.status).toBe('healthy')
      expect(result.label).toBe('Здорова')
    })

    it('includes timeouts in error rate', () => {
      const result = calculateSystemHealth(100, 5, 5)
      expect(result.status).toBe('degraded')
      expect(result.message).toContain('10.0%')
    })
  })

  describe('buildActivityChartData', () => {
    it('returns empty data for empty audit activity with all range', () => {
      const result = buildActivityChartData([], [], [], 'all', getAdaptiveTimeRange)
      expect(result.labels).toEqual([])
      expect(result.datasets).toEqual([])
    })

    it('returns data for minute range', () => {
      const sentMessages = [{ timestamp: new Date().toISOString() }]
      const auditActivity = [{ 'Отправлено': 5, 'Получено': 3 }]
      const result = buildActivityChartData(sentMessages, [], auditActivity, 'minute', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(6)
      expect(result.datasets).toHaveLength(2)
    })

    it('returns data for hour range', () => {
      const auditActivity = [{ 'Отправлено': 5, 'Получено': 3 }]
      const result = buildActivityChartData([], [], auditActivity, 'hour', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(12)
      expect(result.datasets).toHaveLength(2)
    })

    it('returns data for day range', () => {
      const auditActivity = [{ 'Отправлено': 5, 'Получено': 3 }]
      const result = buildActivityChartData([], [], auditActivity, 'day', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(24)
      expect(result.datasets).toHaveLength(2)
    })
  })

  describe('buildDuplicatesChartData', () => {
    it('returns empty data for empty audit activity with all range', () => {
      const result = buildDuplicatesChartData([], 'all', getAdaptiveTimeRange)
      expect(result.labels).toEqual([])
      expect(result.datasets).toEqual([])
    })

    it('returns data for minute range', () => {
      const auditActivity = [{ 'Событие не прошло проверку на идемпотентность': 5 }]
      const result = buildDuplicatesChartData(auditActivity, 'minute', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(6)
      expect(result.datasets).toHaveLength(1)
    })

    it('returns data for hour range', () => {
      const auditActivity = [{ 'Событие не прошло проверку на идемпотентность': 5 }]
      const result = buildDuplicatesChartData(auditActivity, 'hour', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(12)
    })

    it('returns data for day range', () => {
      const auditActivity = [{ 'Событие не прошло проверку на идемпотентность': 5 }]
      const result = buildDuplicatesChartData(auditActivity, 'day', getAdaptiveTimeRange)
      expect(result.labels).toHaveLength(24)
    })

    it('extracts duplicate count from audit activity', () => {
      const auditActivity = [{ 'Событие не прошло проверку на идемпотентность': 10 }]
      const result = buildDuplicatesChartData(auditActivity, 'minute', getAdaptiveTimeRange)
      expect(result.datasets[0].data[0]).toBe(10)
    })
  })

  describe('buildErrorsChartData', () => {
    it('returns empty data for empty audit activity with all range', () => {
      const result = buildErrorsChartData([], 'all', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.labels).toEqual([])
      expect(result.datasets).toEqual([])
    })

    it('returns data for minute range', () => {
      const auditActivity = [
        { 'Не получен асинхронный ответ от системы-получателя вовремя': 5 },
        { 'Некорректное входящее событие': 3 }
      ]
      const result = buildErrorsChartData(auditActivity, 'minute', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.labels).toHaveLength(6)
      expect(result.datasets).toHaveLength(2)
    })

    it('returns data for hour range', () => {
      const auditActivity = [{ 'Не получен асинхронный ответ от системы-получателя вовремя': 5 }]
      const result = buildErrorsChartData(auditActivity, 'hour', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.labels).toHaveLength(12)
    })

    it('returns data for day range', () => {
      const auditActivity = [{ 'Не получен асинхронный ответ от системы-получателя вовремя': 5 }]
      const result = buildErrorsChartData(auditActivity, 'day', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.labels).toHaveLength(24)
    })

    it('separates timeouts and errors', () => {
      const auditActivity = [
        { 'Не получен асинхронный ответ от системы-получателя вовремя': 5 },
        { 'Некорректное входящее событие': 3 }
      ]
      const result = buildErrorsChartData(auditActivity, 'minute', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.datasets[0].label).toBe('Таймауты')
      expect(result.datasets[1].label).toBe('Ошибки')
    })

    it('sums all error types', () => {
      const auditActivity = [
        {
          'Некорректное входящее событие': 2,
          'Не найден маршрут для входящего события': 1,
          'Некорректный ответ от системы-получателя': 1,
          'Получен ответ без ожидающей операции': 1
        }
      ]
      const result = buildErrorsChartData(auditActivity, 'minute', getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
      expect(result.datasets[1].data[0]).toBe(5)
    })
  })

  describe('calculateSinceTimestamp', () => {
    it('returns undefined for all time range', () => {
      const result = calculateSinceTimestamp('all')
      expect(result).toBeUndefined()
    })

    it('returns timestamp 1 minute ago for minute range', () => {
      const result = calculateSinceTimestamp('minute')
      const now = new Date()
      const oneMinuteAgo = new Date(now.getTime() - 60 * 1000)
      const diff = Math.abs(new Date(result).getTime() - oneMinuteAgo.getTime())
      expect(diff).toBeLessThan(1000) // Allow 1 second tolerance
    })

    it('returns timestamp 1 hour ago for hour range', () => {
      const result = calculateSinceTimestamp('hour')
      const now = new Date()
      const oneHourAgo = new Date(now.getTime() - 60 * 60 * 1000)
      const diff = Math.abs(new Date(result).getTime() - oneHourAgo.getTime())
      expect(diff).toBeLessThan(1000) // Allow 1 second tolerance
    })

    it('returns start of day for day range', () => {
      const result = calculateSinceTimestamp('day')
      const now = new Date()
      const startOfDay = new Date(now)
      startOfDay.setHours(0, 0, 0, 0)
      const diff = Math.abs(new Date(result).getTime() - startOfDay.getTime())
      expect(diff).toBeLessThan(1000) // Allow 1 second tolerance
    })

    it('returns valid ISO string', () => {
      const result = calculateSinceTimestamp('minute')
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}Z$/)
    })
  })

  describe('toggleIntegration', () => {
    it('adds integration to set when not present', () => {
      const expanded = ['integration1']
      const result = toggleIntegration(expanded, 'integration2')
      expect(result).toContain('integration2')
      expect(result).toContain('integration1')
    })

    it('removes integration from set when present', () => {
      const expanded = ['integration1', 'integration2']
      const result = toggleIntegration(expanded, 'integration1')
      expect(result).not.toContain('integration1')
      expect(result).toContain('integration2')
    })

    it('returns a Set', () => {
      const expanded = []
      const result = toggleIntegration(expanded, 'integration1')
      expect(result).toBeInstanceOf(Set)
    })
  })

  describe('channelEntries', () => {
    it('returns all channels when all are present', () => {
      const intg = {
        inbound: 'channel1',
        requestOut: 'channel2',
        replyIn: 'channel3',
        replyOut: 'channel4'
      }
      const result = channelEntries(intg)
      expect(result).toHaveLength(4)
      expect(result[0]).toEqual(['Inbound', 'channel1'])
      expect(result[1]).toEqual(['Request Out', 'channel2'])
      expect(result[2]).toEqual(['Reply In', 'channel3'])
      expect(result[3]).toEqual(['Reply Out', 'channel4'])
    })

    it('filters out null channels', () => {
      const intg = {
        inbound: 'channel1',
        requestOut: null,
        replyIn: 'channel3',
        replyOut: null
      }
      const result = channelEntries(intg)
      expect(result).toHaveLength(2)
      expect(result[0]).toEqual(['Inbound', 'channel1'])
      expect(result[1]).toEqual(['Reply In', 'channel3'])
    })

    it('returns empty array when all channels are null', () => {
      const intg = {
        inbound: null,
        requestOut: null,
        replyIn: null,
        replyOut: null
      }
      const result = channelEntries(intg)
      expect(result).toEqual([])
    })

    it('handles missing properties', () => {
      const intg = {
        inbound: 'channel1'
      }
      const result = channelEntries(intg)
      expect(result).toHaveLength(1)
      expect(result[0]).toEqual(['Inbound', 'channel1'])
    })
  })
})
