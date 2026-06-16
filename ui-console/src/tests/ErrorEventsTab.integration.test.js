import { describe, it, expect, vi, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import ErrorEventsTab from '../components/ErrorEventsTab.vue'

vi.mock('../api/index.js', () => ({
  coreClient: {
    get: vi.fn(() => Promise.resolve({ data: { data: { content: [], totalElements: 0 } } })),
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

describe('ErrorEventsTab Integration Tests', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('mounts successfully', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.exists()).toBe(true)
  })

  it('renders without errors', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.find('div').exists()).toBe(true)
  })

  it('has initial loading state', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.loading).toBeDefined()
  })

  it('has errorEvents ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.errorEvents || wrapper.vm.events).toBeDefined()
  })

  it('has totalElements ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.totalElements).toBeDefined()
  })

  it('has currentPage ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.page).toBeDefined()
  })

  it('has pageSize ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.limit).toBeDefined()
  })

  it('has search ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.filterKey).toBeDefined()
  })

  it('has selectedEvent ref', () => {
    const wrapper = mount(ErrorEventsTab, {
      global: {
        stubs: {
          StatusBadge: true,
        },
      },
    })
    expect(wrapper.vm.selectedEvent).toBeDefined()
  })
})
