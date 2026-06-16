import { describe, it, expect, vi, beforeEach } from 'vitest'

vi.mock('../api/index.js', () => ({
  senderClient: {
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('Sender API Module', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('module exports senderApi object', async () => {
    const { senderApi } = await import('../api/sender.js')
    expect(senderApi).toBeDefined()
    expect(typeof senderApi).toBe('object')
  })

  it('sendEvent calls senderClient.post', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    const data = { globalKey: 'test', payload: {} }
    senderApi.sendEvent(data)
    expect(senderClient.post).toHaveBeenCalledWith('/api/sender/send', data)
  })

  it('getSentMessages calls senderClient.get with since param', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getSentMessages('2024-01-01')
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/sent', { params: { since: '2024-01-01' } })
  })

  it('getSentMessages calls senderClient.get without since param', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getSentMessages()
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/sent', { params: {} })
  })

  it('getReplies calls senderClient.get', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getReplies()
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/replies')
  })

  it('getStats calls senderClient.get with since param', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getStats('2024-01-01')
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/stats', { params: { since: '2024-01-01' } })
  })

  it('getStats calls senderClient.get without since param', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getStats()
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/stats', { params: {} })
  })

  it('resetState calls senderClient.delete', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.resetState()
    expect(senderClient.delete).toHaveBeenCalledWith('/api/sender/state')
  })

  it('getSimulationConfig calls senderClient.get', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    senderApi.getSimulationConfig()
    expect(senderClient.get).toHaveBeenCalledWith('/api/sender/config')
  })

  it('updateSimulationConfig calls senderClient.put', async () => {
    const { senderApi } = await import('../api/sender.js')
    const { senderClient } = await import('../api/index.js')
    
    const data = { enabled: true }
    senderApi.updateSimulationConfig(data)
    expect(senderClient.put).toHaveBeenCalledWith('/api/sender/config', data)
  })
})
