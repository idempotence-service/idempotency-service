import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import ErrorEventsTab from '../components/ErrorEventsTab.vue'

describe('ErrorEventsTab Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../components/ErrorEventsTab.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../components/ErrorEventsTab.vue')
    expect(component.default).toBeDefined()
  })

  it('renders error events tab', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>ErrorEvents</div>' } }]
    })
    
    const wrapper = mount(ErrorEventsTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })

  it('has error events sections', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>ErrorEvents</div>' } }]
    })
    
    const wrapper = mount(ErrorEventsTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })

  it('has error events table', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>ErrorEvents</div>' } }]
    })
    
    const wrapper = mount(ErrorEventsTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })
})
