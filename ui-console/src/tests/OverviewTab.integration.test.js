import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import OverviewTab from '../components/OverviewTab.vue'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
    put: vi.fn(() => Promise.resolve({ data: { success: true } })),
  },
  senderClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
  },
  receiverClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: [] } })),
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

describe('OverviewTab Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts successfully', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('renders without errors', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.find('div').exists()).toBe(true)
  })

  it('has initial loading state', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('has activityTimeRange ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.activityTimeRange).toBeDefined()
  })

  it('has errorEvents ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.errorEvents).toBeDefined()
  })

  it('has sentCount ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.sentCount).toBeDefined()
  })

  it('has receivedCount ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.receivedCount).toBeDefined()
  })

  it('has duplicateCount ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.duplicateCount).toBeDefined()
  })

  it('has timeoutCount ref', () => {
    const wrapper = mount(OverviewTab, {
      global: {
        stubs: {
          StatusBadge: true,
          Doughnut: true,
          Line: true,
        },
      },
    })
    expect(wrapper.vm.timeoutCount).toBeDefined()
  })
})
