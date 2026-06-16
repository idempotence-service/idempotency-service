import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import LoginView from '../views/LoginView.vue'
import { useAuthStore } from '../stores/auth.js'

describe('LoginView Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('component can be imported', async () => {
    const component = await import('../views/LoginView.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../views/LoginView.vue')
    expect(component.default).toBeDefined()
  })

  it('renders login form', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.find('form').exists()).toBe(true)
  })

  it('has token input field', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.find('input[type="password"]').exists()).toBe(true)
  })

  it('has submit button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.find('button[type="submit"]').exists()).toBe(true)
  })

  it('toggles password visibility', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      }
    })
    
    const toggleButton = wrapper.findAll('button')[0]
    await toggleButton.trigger('click')
    
    expect(wrapper.find('input[type="text"]').exists()).toBe(true)
  })

  it('displays error message when error is set', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      },
      data() {
        return { error: 'Test error' }
      }
    })
    
    expect(wrapper.text()).toContain('Test error')
  })

  it('disables button when loading', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      },
      data() {
        return { loading: true }
      }
    })
    
    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
  })

  it('disables button when token is empty', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      },
      data() {
        return { token: '' }
      }
    })
    
    const submitButton = wrapper.find('button[type="submit"]')
    expect(submitButton.attributes('disabled')).toBeDefined()
  })

  it('shows loading spinner when loading', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      },
      data() {
        return { loading: true }
      }
    })
    
    expect(wrapper.find('.animate-spin').exists()).toBe(true)
  })

  it('shows "Войти" text when not loading', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Login</div>' } }]
    })
    
    const wrapper = mount(LoginView, {
      global: {
        plugins: [router]
      },
      data() {
        return { loading: false }
      }
    })
    
    expect(wrapper.text()).toContain('Войти')
  })
})
