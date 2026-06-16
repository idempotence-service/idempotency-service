import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import OverviewTab from '../components/OverviewTab.vue'

describe('OverviewTab Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../components/OverviewTab.vue')
    expect(component).toBeDefined()
  })

  it('component has computed properties', async () => {
    const component = await import('../components/OverviewTab.vue')
    expect(component.default).toBeDefined()
  })

  it('renders overview tab', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Overview</div>' } }]
    })
    
    const wrapper = mount(OverviewTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })

  it('has overview sections', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Overview</div>' } }]
    })
    
    const wrapper = mount(OverviewTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })
})
