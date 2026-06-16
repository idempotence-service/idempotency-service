import { describe, it, expect } from 'vitest'

describe('API Index Module', () => {
  it('module exports clients', async () => {
    const { coreClient, senderClient, receiverClient } = await import('../api/index.js')
    expect(coreClient).toBeDefined()
    expect(senderClient).toBeDefined()
    expect(receiverClient).toBeDefined()
  })
})
