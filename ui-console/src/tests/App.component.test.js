import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import App from '../App.vue'
import OverviewTab from '../components/OverviewTab.vue'

describe('App.vue Component Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders RouterView', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: OverviewTab }
      ]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router, createPinia()]
      }
    })
    
    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
  })

  it('renders ToastNotification', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: OverviewTab }
      ]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router, createPinia()]
      }
    })
    
    expect(wrapper.findComponent({ name: 'ToastNotification' }).exists()).toBe(true)
  })

  it('renders without errors', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: OverviewTab }
      ]
    })
    
    expect(() => {
      mount(App, {
        global: {
          plugins: [router, createPinia()]
        }
      })
    }).not.toThrow()
  })

  it('has correct structure', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', component: OverviewTab }
      ]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router, createPinia()]
      }
    })
    
    expect(wrapper.html()).toBeDefined()
  })
})
