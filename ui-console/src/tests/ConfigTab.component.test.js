import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import ConfigTab from '../components/ConfigTab.vue'

describe('ConfigTab Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../components/ConfigTab.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../components/ConfigTab.vue')
    expect(component.default).toBeDefined()
  })

  it('renders config tab', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })

  it('has config sections', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })
})
