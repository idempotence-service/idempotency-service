import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  },
}))

describe('Core API Module', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('module exports coreApi object', async () => {
    const { coreApi } = await import('../api/core.js')
    expect(coreApi).toBeDefined()
    expect(typeof coreApi).toBe('object')
  })

  it('getErrorEvents calls coreClient.get with correct endpoint', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getErrorEvents({ page: 0, limit: 10 })
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-error-events', { params: { page: 0, limit: 10 } })
  })

  it('getDuplicateEvents calls coreClient.get with default params', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getDuplicateEvents()
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-duplicate-events', { params: { page: 0, limit: 100 } })
  })

  it('getDuplicateEvents calls coreClient.get with custom params', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getDuplicateEvents(1, 50)
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-duplicate-events', { params: { page: 1, limit: 50 } })
  })

  it('getDuplicateCount calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getDuplicateCount()
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-duplicate-count')
  })

  it('getTimeoutCount calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getTimeoutCount()
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-timeout-count')
  })

  it('getAuditActivity calls coreClient.get with since param', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getAuditActivity('2024-01-01')
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-audit-activity', { params: { since: '2024-01-01' } })
  })

  it('restartEvent calls coreClient.post', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.restartEvent('test-key')
    expect(coreClient.post).toHaveBeenCalledWith('/core/restart-event', { globalKey: 'test-key' })
  })

  it('getEventById calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getEventById('test-key')
    expect(coreClient.get).toHaveBeenCalledWith('/core/get-event-by-id', { params: { globalKey: 'test-key' } })
  })

  it('getConfig calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getConfig()
    expect(coreClient.get).toHaveBeenCalledWith('/config')
  })

  it('updateScheduler calls coreClient.put', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    const data = { outboxFixedDelaySeconds: 5 }
    coreApi.updateScheduler(data)
    expect(coreClient.put).toHaveBeenCalledWith('/config/scheduler', data)
  })

  it('updateResilience calls coreClient.put', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    const data = { retryInitialDelayMs: 1000 }
    coreApi.updateResilience(data)
    expect(coreClient.put).toHaveBeenCalledWith('/config/resilience', data)
  })

  it('updateCleanup calls coreClient.put', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    const data = { cleanupRetentionDays: 7 }
    coreApi.updateCleanup(data)
    expect(coreClient.put).toHaveBeenCalledWith('/config/cleanup', data)
  })

  it('updateListener calls coreClient.put', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    const data = { inboundConcurrency: 5 }
    coreApi.updateListener(data)
    expect(coreClient.put).toHaveBeenCalledWith('/config/listener', data)
  })

  it('getIntegrations calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getIntegrations()
    expect(coreClient.get).toHaveBeenCalledWith('/config/integrations')
  })

  it('getEnabledIntegrations calls coreClient.get', async () => {
    const { coreApi } = await import('../api/core.js')
    const { coreClient } = await import('../api/index.js')
    
    coreApi.getEnabledIntegrations()
    expect(coreClient.get).toHaveBeenCalledWith('/config/integrations/enabled')
  })
})
