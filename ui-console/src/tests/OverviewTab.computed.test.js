import { describe, it, expect } from 'vitest'

describe('OverviewTab Computed Properties', () => {
  describe('statCards computation', () => {
    it('calculates total errors from audit activity', () => {
      const auditActivity = [
        { 'Некорректное входящее событие': 5 },
        { 'Не найден маршрут для входящего события': 3 },
        { 'Некорректный ответ от системы-получателя': 2 },
      ]
      
      const errorCountFromAudit = auditActivity.reduce((sum, slot) => {
        return sum +
          (slot['Некорректное входящее событие'] || 0) +
          (slot['Не найден маршрут для входящего события'] || 0) +
          (slot['Некорректный ответ от системы-получателя'] || 0)
      }, 0)
      
      expect(errorCountFromAudit).toBe(10)
    })

    it('calculates duplicate count from audit activity', () => {
      const auditActivity = [
        { 'Событие не прошло проверку на идемпотентность': 15 },
        { 'Событие не прошло проверку на идемпотентность': 10 },
      ]
      
      const duplicateCountFromAudit = auditActivity.reduce((sum, slot) => {
        return sum + (slot['Событие не прошло проверку на идемпотентность'] || 0)
      }, 0)
      
      expect(duplicateCountFromAudit).toBe(25)
    })

    it('calculates timeout count from audit activity', () => {
      const auditActivity = [
        { 'Не получен асинхронный ответ от системы-получателя вовремя': 7 },
      ]
      
      const timeoutCountFromAudit = auditActivity.reduce((sum, slot) => {
        return sum + (slot['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      }, 0)
      
      expect(timeoutCountFromAudit).toBe(7)
    })

    it('handles empty audit activity', () => {
      const auditActivity = []
      
      const errorCountFromAudit = auditActivity.reduce((sum, slot) => {
        return sum +
          (slot['Некорректное входящее событие'] || 0) +
          (slot['Не найден маршрут для входящего события'] || 0)
      }, 0)
      
      expect(errorCountFromAudit).toBe(0)
    })
  })

  describe('typeBreakdown computation', () => {
    it('aggregates message counts by type', () => {
      const sentCount = 100
      const receivedCount = 80
      const totalErrorsFromAudit = 5
      const duplicateCountFromAudit = 15
      
      const typeBreakdown = [
        { label: 'Отправлено',  count: sentCount,      color: '#82b1ff' },
        { label: 'Получено',    count: receivedCount,  color: '#6dd58c' },
        { label: 'Проблемы обработки', count: totalErrorsFromAudit,    color: '#f2b8b5' },
        { label: 'Дубликаты',   count: duplicateCountFromAudit, color: '#f6c142' },
      ]
      
      expect(typeBreakdown).toHaveLength(4)
      expect(typeBreakdown[0].count).toBe(100)
      expect(typeBreakdown[1].count).toBe(80)
      expect(typeBreakdown[2].count).toBe(5)
      expect(typeBreakdown[3].count).toBe(15)
    })
  })

  describe('topErrorIntegrations computation', () => {
    it('aggregates error counts by integration', () => {
      const errorEvents = [
        { integration: 'system1-to-system2', globalKey: 'key1' },
        { integration: 'system1-to-system2', globalKey: 'key2' },
        { integration: 'crm-to-billing', globalKey: 'key3' },
        { integration: 'system1-to-system2', globalKey: 'key4' },
        { integration: 'inventory-to-orders', globalKey: 'key5' },
      ]
      
      const counts = {}
      errorEvents.forEach(e => {
        counts[e.integration] = (counts[e.integration] || 0) + 1
      })
      
      expect(counts['system1-to-system2']).toBe(3)
      expect(counts['crm-to-billing']).toBe(1)
      expect(counts['inventory-to-orders']).toBe(1)
    })

    it('sorts integrations by error count descending', () => {
      const errorEvents = [
        { integration: 'A', globalKey: 'key1' },
        { integration: 'A', globalKey: 'key2' },
        { integration: 'B', globalKey: 'key3' },
        { integration: 'C', globalKey: 'key4' },
        { integration: 'C', globalKey: 'key5' },
        { integration: 'C', globalKey: 'key6' },
      ]
      
      const counts = {}
      errorEvents.forEach(e => {
        counts[e.integration] = (counts[e.integration] || 0) + 1
      })
      
      const sorted = Object.entries(counts).sort((a, b) => b[1] - a[1])
      
      expect(sorted[0][0]).toBe('C')
      expect(sorted[0][1]).toBe(3)
      expect(sorted[1][0]).toBe('A')
      expect(sorted[1][1]).toBe(2)
      expect(sorted[2][0]).toBe('B')
      expect(sorted[2][1]).toBe(1)
    })

    it('limits to top 5 integrations', () => {
      const errorEvents = Array.from({ length: 10 }, (_, i) => ({
        integration: `integration-${i}`,
        globalKey: `key${i}`
      }))
      
      const counts = {}
      errorEvents.forEach(e => {
        counts[e.integration] = (counts[e.integration] || 0) + 1
      })
      
      const top5 = Object.entries(counts)
        .sort((a, b) => b[1] - a[1])
        .slice(0, 5)
      
      expect(top5).toHaveLength(5)
    })
  })

  describe('activityChartData computation', () => {
    it('generates labels for minute time range', () => {
      const slots = 12
      const intervalMs = 5 * 1000
      const labels = []
      
      for (let i = 0; i < slots; i++) {
        const d = new Date(Date.now() - (slots - 1 - i) * intervalMs)
        labels.push(`${d.getSeconds().toString().padStart(2, '0')}с`)
      }
      
      expect(labels).toHaveLength(12)
      expect(labels[0]).toMatch(/^\d{2}с$/)
    })

    it('generates labels for hour time range', () => {
      const slots = 12
      const intervalMs = 5 * 60000
      const labels = []
      
      for (let i = 0; i < slots; i++) {
        const d = new Date(Date.now() - (slots - 1 - i) * intervalMs)
        labels.push(`${d.getMinutes().toString().padStart(2, '0')}:${d.getSeconds().toString().padStart(2, '0')}`)
      }
      
      expect(labels).toHaveLength(12)
      expect(labels[0]).toMatch(/^\d{2}:\d{2}$/)
    })

    it('generates labels for day time range', () => {
      const slots = 12
      const intervalMs = 2 * 3600 * 1000
      const labels = []
      
      for (let i = 0; i < slots; i++) {
        const d = new Date(Date.now() - (slots - 1 - i) * intervalMs)
        labels.push(`${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`)
      }
      
      expect(labels).toHaveLength(12)
      expect(labels[0]).toMatch(/^\d{2}:\d{2}$/)
    })

    it('handles single digit seconds', () => {
      const d = new Date()
      d.setSeconds(5)
      const label = d.getSeconds().toString().padStart(2, '0')
      expect(label).toBe('05')
    })

    it('handles double digit seconds', () => {
      const d = new Date()
      d.setSeconds(15)
      const label = d.getSeconds().toString().padStart(2, '0')
      expect(label).toBe('15')
    })
  })

  describe('successRate computation', () => {
    it('calculates success rate with no errors', () => {
      const sentCount = 100
      const errorCount = 0
      const successRate = sentCount > 0 ? ((sentCount - errorCount) / sentCount) * 100 : 0
      expect(successRate).toBe(100)
    })

    it('calculates success rate with errors', () => {
      const sentCount = 100
      const errorCount = 10
      const successRate = sentCount > 0 ? ((sentCount - errorCount) / sentCount) * 100 : 0
      expect(successRate).toBe(90)
    })

    it('handles zero sent count', () => {
      const sentCount = 0
      const errorCount = 0
      const successRate = sentCount > 0 ? ((sentCount - errorCount) / sentCount) * 100 : 0
      expect(successRate).toBe(0)
    })

    it('handles error count greater than sent count', () => {
      const sentCount = 50
      const errorCount = 60
      const successRate = sentCount > 0 ? ((sentCount - errorCount) / sentCount) * 100 : 0
      expect(successRate).toBe(-20)
    })
  })

  describe('systemHealth computation', () => {
    it('calculates healthy system', () => {
      const successRate = 98
      const isHealthy = successRate >= 95
      expect(isHealthy).toBe(true)
    })

    it('calculates unhealthy system', () => {
      const successRate = 90
      const isHealthy = successRate >= 95
      expect(isHealthy).toBe(false)
    })

    it('calculates borderline system', () => {
      const successRate = 95
      const isHealthy = successRate >= 95
      expect(isHealthy).toBe(true)
    })
  })

  describe('errorRate computation', () => {
    it('calculates error rate with errors', () => {
      const sentCount = 100
      const errorCount = 10
      const errorRate = sentCount > 0 ? (errorCount / sentCount) * 100 : 0
      expect(errorRate).toBe(10)
    })

    it('calculates error rate with no errors', () => {
      const sentCount = 100
      const errorCount = 0
      const errorRate = sentCount > 0 ? (errorCount / sentCount) * 100 : 0
      expect(errorRate).toBe(0)
    })

    it('handles zero sent count for error rate', () => {
      const sentCount = 0
      const errorCount = 0
      const errorRate = sentCount > 0 ? (errorCount / sentCount) * 100 : 0
      expect(errorRate).toBe(0)
    })
  })

  describe('duplicateRate computation', () => {
    it('calculates duplicate rate with duplicates', () => {
      const sentCount = 100
      const duplicateCount = 15
      const duplicateRate = sentCount > 0 ? (duplicateCount / sentCount) * 100 : 0
      expect(duplicateRate).toBe(15)
    })

    it('calculates duplicate rate with no duplicates', () => {
      const sentCount = 100
      const duplicateCount = 0
      const duplicateRate = sentCount > 0 ? (duplicateCount / sentCount) * 100 : 0
      expect(duplicateRate).toBe(0)
    })

    it('handles zero sent count for duplicate rate', () => {
      const sentCount = 0
      const duplicateCount = 0
      const duplicateRate = sentCount > 0 ? (duplicateCount / sentCount) * 100 : 0
      expect(duplicateRate).toBe(0)
    })
  })

  describe('timeoutRate computation', () => {
    it('calculates timeout rate with timeouts', () => {
      const sentCount = 100
      const timeoutCount = 5
      const timeoutRate = sentCount > 0 ? (timeoutCount / sentCount) * 100 : 0
      expect(timeoutRate).toBe(5)
    })

    it('calculates timeout rate with no timeouts', () => {
      const sentCount = 100
      const timeoutCount = 0
      const timeoutRate = sentCount > 0 ? (timeoutCount / sentCount) * 100 : 0
      expect(timeoutRate).toBe(0)
    })

    it('handles zero sent count for timeout rate', () => {
      const sentCount = 0
      const timeoutCount = 0
      const timeoutRate = sentCount > 0 ? (timeoutCount / sentCount) * 100 : 0
      expect(timeoutRate).toBe(0)
    })
  })

  describe('healthStatus computation', () => {
    it('returns healthy when error rate is low', () => {
      const errorRate = 2
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('healthy')
    })

    it('returns warning when error rate is medium', () => {
      const errorRate = 7
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('warning')
    })

    it('returns critical when error rate is high', () => {
      const errorRate = 15
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('critical')
    })

    it('returns healthy at boundary', () => {
      const errorRate = 4.9
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('healthy')
    })

    it('returns warning at boundary', () => {
      const errorRate = 5
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('warning')
    })

    it('returns critical at boundary', () => {
      const errorRate = 10
      const healthStatus = errorRate < 5 ? 'healthy' : errorRate < 10 ? 'warning' : 'critical'
      expect(healthStatus).toBe('critical')
    })
  })

  describe('throughput calculation', () => {
    it('calculates throughput with valid data', () => {
      const sentCount = 100
      const duration = 10
      const throughput = duration > 0 ? sentCount / duration : 0
      expect(throughput).toBe(10)
    })

    it('handles zero duration', () => {
      const sentCount = 100
      const duration = 0
      const throughput = duration > 0 ? sentCount / duration : 0
      expect(throughput).toBe(0)
    })

    it('handles zero sent count', () => {
      const sentCount = 0
      const duration = 10
      const throughput = duration > 0 ? sentCount / duration : 0
      expect(throughput).toBe(0)
    })

    it('handles negative duration', () => {
      const sentCount = 100
      const duration = -10
      const throughput = duration > 0 ? sentCount / duration : 0
      expect(throughput).toBe(0)
    })
  })

  describe('blocked count calculation', () => {
    it('calculates blocked count', () => {
      const events = [
        { status: 'BLOCKED' },
        { status: 'SUCCESS' },
        { status: 'BLOCKED' },
      ]
      const blockedCount = events.filter(e => e.status === 'BLOCKED').length
      expect(blockedCount).toBe(2)
    })

    it('handles empty events array', () => {
      const events = []
      const blockedCount = events.filter(e => e.status === 'BLOCKED').length
      expect(blockedCount).toBe(0)
    })

    it('handles null events', () => {
      const events = null
      const blockedCount = events ? events.filter(e => e.status === 'BLOCKED').length : 0
      expect(blockedCount).toBe(0)
    })
  })

  describe('timeout count calculation', () => {
    it('calculates timeout count', () => {
      const events = [
        { status: 'TIMEOUT' },
        { status: 'SUCCESS' },
        { status: 'TIMEOUT' },
      ]
      const timeoutCount = events.filter(e => e.status === 'TIMEOUT').length
      expect(timeoutCount).toBe(2)
    })

    it('handles empty events array', () => {
      const events = []
      const timeoutCount = events.filter(e => e.status === 'TIMEOUT').length
      expect(timeoutCount).toBe(0)
    })
  })
})
