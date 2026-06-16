import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import App from '../App.vue'

describe('App Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders app component', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })

  it('has router-view', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
  })

  it('can be imported', async () => {
    const app = await import('../App.vue')
    expect(app).toBeDefined()
  })

  it('has default export', async () => {
    const app = await import('../App.vue')
    expect(app.default).toBeDefined()
  })
})
