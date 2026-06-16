import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(),
  },
  senderClient: {
    get: vi.fn(),
  },
  receiverClient: {
    get: vi.fn(),
  },
}))

describe('OverviewTab Async Functions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loadErrors calls correct endpoint', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockResolvedValue({ data: { data: { content: [], totalElements: 0 } } })

    const { coreApi } = await import('../api/core.js')
    await coreApi.getErrorEvents({ page: 0, limit: 100, sort: 'desc' })

    expect(coreClient.get).toHaveBeenCalledWith('/core/get-error-events', { params: { page: 0, limit: 100, sort: 'desc' } })
  })

  it('loadSenderStats calls correct endpoint with since param', async () => {
    const { senderClient } = await import('../api/index.js')
    senderClient.get.mockResolvedValue({ data: { data: { totalSent: 100, totalReplies: 95 } } })

    const { senderApi } = await import('../api/sender.js')
    await senderApi.getStats('2024-01-01T00:00:00Z')

    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/stats', { params: { since: '2024-01-01T00:00:00Z' } })
  })

  it('loadSenderStats calls without since param when undefined', async () => {
    const { senderClient } = await import('../api/index.js')
    senderClient.get.mockResolvedValue({ data: { data: { totalSent: 100, totalReplies: 95 } } })

    const { senderApi } = await import('../api/sender.js')
    await senderApi.getStats()

    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/stats', { params: {} })
  })

  it('loadReceiverStats calls correct endpoint with since param', async () => {
    const { receiverClient } = await import('../api/index.js')
    receiverClient.get.mockResolvedValue({ data: { data: { totalReceived: 95 } } })

    const { receiverApi } = await import('../api/receiver.js')
    await receiverApi.getStats('2024-01-01T00:00:00Z')

    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/stats', { params: { since: '2024-01-01T00:00:00Z' } })
  })

  it('loadReceiverStats calls without since param when undefined', async () => {
    const { receiverClient } = await import('../api/index.js')
    receiverClient.get.mockResolvedValue({ data: { data: { totalReceived: 95 } } })

    const { receiverApi } = await import('../api/receiver.js')
    await receiverApi.getStats()

    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/stats', { params: {} })
  })

  it('loadDuplicates calls correct endpoint', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockResolvedValue({ data: { data: 10 } })

    const { coreApi } = await import('../api/core.js')
    await coreApi.getDuplicateCount()

    expect(coreClient.get).toHaveBeenCalledWith('/core/get-duplicate-count')
  })

  it('loadTimeouts calls correct endpoint', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockResolvedValue({ data: { data: 5 } })

    const { coreApi } = await import('../api/core.js')
    await coreApi.getTimeoutCount()

    expect(coreClient.get).toHaveBeenCalledWith('/core/get-timeout-count')
  })

  it('loadIntegrations calls correct endpoint', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockResolvedValue({ data: { data: [] } })

    const { coreApi } = await import('../api/core.js')
    await coreApi.getIntegrations()

    expect(coreClient.get).toHaveBeenCalledWith('/config/integrations')
  })

  it('loadSentMessages calls correct endpoint with since param', async () => {
    const { senderClient } = await import('../api/index.js')
    senderClient.get.mockResolvedValue({ data: { data: [] } })

    const { senderApi } = await import('../api/sender.js')
    await senderApi.getSentMessages('2024-01-01T00:00:00Z')

    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/sent', { params: { since: '2024-01-01T00:00:00Z' } })
  })

  it('loadReceivedMessages calls correct endpoint with since param', async () => {
    const { receiverClient } = await import('../api/index.js')
    receiverClient.get.mockResolvedValue({ data: { data: [] } })

    const { receiverApi } = await import('../api/receiver.js')
    await receiverApi.getReceivedEvents('2024-01-01T00:00:00Z')

    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/events', { params: { since: '2024-01-01T00:00:00Z' } })
  })
})
