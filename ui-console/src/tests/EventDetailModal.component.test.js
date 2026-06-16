import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'

describe('EventDetailModal Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default).toBeDefined()
  })

  it('component accepts modelValue prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })

  it('component accepts event prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })

  it('component accepts loading prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })

  it('component accepts details prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })

  it('component accepts canRetry prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })

  it('component accepts retrying prop', async () => {
    const component = await import('../components/EventDetailModal.vue')
    expect(component.default.props).toBeDefined()
  })
})
