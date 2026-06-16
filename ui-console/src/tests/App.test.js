import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { createPinia, setActivePinia } from 'pinia'
import App from '../App.vue'
import ToastNotification from '../components/ToastNotification.vue'

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
      },
      attachTo: document.body
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
      },
      attachTo: document.body
    })
    
    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
  })

  it('renders ToastNotification component', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router]
      },
      attachTo: document.body
    })
    
    expect(wrapper.findComponent(ToastNotification).exists()).toBe(true)
  })

  it('renders both RouterView and ToastNotification', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Home</div>' } }]
    })
    
    const wrapper = mount(App, {
      global: {
        plugins: [router]
      },
      attachTo: document.body
    })
    
    expect(wrapper.findComponent({ name: 'RouterView' }).exists()).toBe(true)
    expect(wrapper.findComponent(ToastNotification).exists()).toBe(true)
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
