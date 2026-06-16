import { describe, it, expect } from 'vitest'

describe('Router Module', () => {
  it('router can be imported', async () => {
    const router = await import('../router/index.js')
    expect(router).toBeDefined()
  })

  it('router has default export', async () => {
    const router = await import('../router/index.js')
    expect(router.default).toBeDefined()
  })
})
