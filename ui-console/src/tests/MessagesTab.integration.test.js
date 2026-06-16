import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import MessagesTab from '../components/MessagesTab.vue'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: { success: true } })),
  },
  senderClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    post: vi.fn(() => Promise.resolve({ data: { success: true } })),
  },
  receiverClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
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

describe('MessagesTab Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts successfully', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('renders without errors', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.find('div').exists()).toBe(true)
  })

  it('has initial loading state', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    // Component should have loading ref
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('has search ref', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.search).toBeDefined()
  })

  it('has activeFilter ref', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.activeFilter).toBeDefined()
  })

  it('has toggleSortLocal function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.toggleSortLocal).toBe('function')
  })

  it('has handleScroll function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.handleScroll).toBe('function')
  })

  it('has copyLocal function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.copyLocal).toBe('function')
  })

  it('has rebuildMessagesLocal function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.rebuildMessagesLocal).toBe('function')
  })

  it('has loadAll function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.loadAll).toBe('function')
  })

  it('has retryEvent function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.retryEvent).toBe('function')
  })

  it('has loadAuditActivity function', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(typeof wrapper.vm.loadAuditActivity).toBe('function')
  })

  it('toggleSortLocal can be called without error', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(() => wrapper.vm.toggleSortLocal('timestamp')).not.toThrow()
  })

  it('rebuildMessagesLocal can be called without error', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(() => wrapper.vm.rebuildMessagesLocal()).not.toThrow()
  })

  it('copyLocal can be called without error', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(() => wrapper.vm.copyLocal('test-key')).not.toThrow()
  })

  it('loadAuditActivity can be called without error', async () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    await expect(wrapper.vm.loadAuditActivity()).resolves.not.toThrow()
  })

  it('loadAll can be called without error', async () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    await expect(wrapper.vm.loadAll()).resolves.not.toThrow()
  })

  it('retryEvent can be called without error', async () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    await expect(wrapper.vm.retryEvent('test-key')).resolves.not.toThrow()
  })

  it('handleScroll can be called without error', () => {
    const wrapper = mount(MessagesTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(() => wrapper.vm.handleScroll()).not.toThrow()
  })
})
