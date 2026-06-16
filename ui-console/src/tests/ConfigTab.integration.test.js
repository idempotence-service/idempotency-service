import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ConfigTab from '../components/ConfigTab.vue'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: {} } })),
    put: vi.fn(() => Promise.resolve({ data: { success: true } })),
  },
}))

vi.mock('../api/sender.js', async (importOriginal) => {
  const actual = await importOriginal()
  return {
    ...actual,
    senderClient: {
      get: vi.fn(() => Promise.resolve({ data: { data: {} } })),
      put: vi.fn(() => Promise.resolve({ data: { success: true } })),
    },
  }
})

vi.mock('../stores/toast.js', () => ({
  useToastStore: () => ({
    toasts: [],
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
    remove: vi.fn(),
  }),
}))

describe('ConfigTab Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts successfully', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.exists()).toBe(true)
  })

  it('renders without errors', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.find('div').exists()).toBe(true)
  })

  it('has initial loading state', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('has scheduler reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.scheduler).toBeDefined()
  })

  it('has resilience reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.resilience).toBeDefined()
  })

  it('has cleanup reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.cleanup).toBeDefined()
  })

  it('has listener reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.listener).toBeDefined()
  })

  it('has simulation reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.simulation).toBeDefined()
  })

  it('has saving reactive object', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.saving).toBeDefined()
  })

  it('has availableIntegrations ref', () => {
    const wrapper = mount(ConfigTab)
    expect(wrapper.vm.availableIntegrations).toBeDefined()
  })
})
