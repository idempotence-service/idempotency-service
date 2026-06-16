import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import EventDetailModal from '../components/EventDetailModal.vue'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: {} } })),
    post: vi.fn(() => Promise.resolve({ data: { success: true } })),
  },
}))

vi.mock('../stores/toast.js', () => ({
  useToastStore: () => ({
    toasts: [],
    success: vi.fn(),
    error: vi.fn(),
    info: vi.fn(),
    remove: vi.fn(),
  }),
}))

describe('EventDetailModal Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts successfully with event prop', () => {
    const wrapper = mount(EventDetailModal, {
      props: {
        show: true,
        event: {
          globalKey: 'test-key',
          status: 'ERROR',
          timestamp: '2024-01-01T00:00:00Z',
        },
      },
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('has event prop', () => {
    const wrapper = mount(EventDetailModal, {
      props: {
        show: true,
        event: {
          globalKey: 'test-key',
          status: 'ERROR',
          timestamp: '2024-01-01T00:00:00Z',
        },
      },
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.props().event).toBeDefined()
  })

  it('has loading ref', () => {
    const wrapper = mount(EventDetailModal, {
      props: {
        show: true,
        event: {
          globalKey: 'test-key',
          status: 'ERROR',
          timestamp: '2024-01-01T00:00:00Z',
        },
      },
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('has retrying ref', () => {
    const wrapper = mount(EventDetailModal, {
      props: {
        show: true,
        event: {
          globalKey: 'test-key',
          status: 'ERROR',
          timestamp: '2024-01-01T00:00:00Z',
        },
      },
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.retrying).toBeDefined()
  })
})
