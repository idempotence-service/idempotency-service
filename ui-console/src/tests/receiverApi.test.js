import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  receiverClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('Receiver API Module', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('module exports receiverApi object', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    expect(receiverApi).toBeDefined()
    expect(typeof receiverApi).toBe('object')
  })

  it('getReceivedEvents calls receiverClient.get with since param', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    receiverApi.getReceivedEvents('2024-01-01')
    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/events', { params: { since: '2024-01-01' } })
  })

  it('getReceivedEvents calls receiverClient.get without since param', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    receiverApi.getReceivedEvents()
    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/events', { params: {} })
  })

  it('getStats calls receiverClient.get with since param', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    receiverApi.getStats('2024-01-01')
    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/stats', { params: { since: '2024-01-01' } })
  })

  it('getStats calls receiverClient.get without since param', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    receiverApi.getStats()
    expect(receiverClient.get).toHaveBeenCalledWith('/api/receiver/stats', { params: {} })
  })

  it('setMode calls receiverClient.post', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    const data = { mode: 'auto' }
    receiverApi.setMode(data)
    expect(receiverClient.post).toHaveBeenCalledWith('/api/receiver/mode', data)
  })

  it('sendManualReply calls receiverClient.post', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    const data = { globalKey: 'test', result: {} }
    receiverApi.sendManualReply(data)
    expect(receiverClient.post).toHaveBeenCalledWith('/api/receiver/reply', data)
  })

  it('resetState calls receiverClient.delete', async () => {
    const { receiverApi } = await import('../api/receiver.js')
    const { receiverClient } = await import('../api/index.js')
    
    receiverApi.resetState()
    expect(receiverClient.delete).toHaveBeenCalledWith('/api/receiver/state')
  })
})
