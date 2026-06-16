import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(),
    post: vi.fn(),
  },
}))

vi.mock('../api/sender.js', () => ({
  senderClient: {
    get: vi.fn(),
  },
}))

vi.mock('../api/receiver.js', () => ({
  receiverClient: {
    get: vi.fn(),
  },
}))

describe('MessagesTab Async Functions', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('loadAuditActivity calculates correct timestamp', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockResolvedValue({ data: { data: [] } })

    const { calculateAuditActivitySince } = await import('../utils/messagesHelpers.js')
    const since = calculateAuditActivitySince()

    const { coreApi } = await import('../api/core.js')
    await coreApi.getAuditActivity(since)

    expect(coreClient.get).toHaveBeenCalledWith('/core/get-audit-activity', { params: { since } })
  })

  it('loadAuditActivity handles errors gracefully', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.get.mockRejectedValue(new Error('Network error'))

    const { calculateAuditActivitySince } = await import('../utils/messagesHelpers.js')
    const since = calculateAuditActivitySince()

    const { coreApi } = await import('../api/core.js')
    
    try {
      await coreApi.getAuditActivity(since)
    } catch (e) {
      expect(e.message).toBe('Network error')
    }
  })

  it('restartEvent calls correct endpoint with globalKey', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.post.mockResolvedValue({ data: { success: true } })

    const { coreApi } = await import('../api/core.js')
    await coreApi.restartEvent('test-key-123')

    expect(coreClient.post).toHaveBeenCalledWith('/core/restart-event', { globalKey: 'test-key-123' })
  })

  it('restartEvent handles error response', async () => {
    const { coreClient } = await import('../api/index.js')
    coreClient.post.mockResolvedValue({ data: { success: false, error: { text: 'Event not found' } } })

    const { coreApi } = await import('../api/core.js')
    const result = await coreApi.restartEvent('test-key-123')

    expect(result.data.success).toBe(false)
    expect(result.data.error.text).toBe('Event not found')
  })
})
