import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import DashboardView from '../views/DashboardView.vue'

describe('DashboardView Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../views/DashboardView.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../views/DashboardView.vue')
    expect(component.default).toBeDefined()
  })

  it('renders dashboard', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.find('.min-h-screen').exists()).toBe(true)
  })

  it('has header with brand', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Idempotency Console')
  })

  it('has tab navigation', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.findAll('button').length).toBeGreaterThan(0)
  })

  it('has overview tab button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Обзор')
  })

  it('has messages tab button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Сообщения')
  })

  it('has config tab button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Конфигурация')
  })

  it('has error events tab button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Ошибки')
  })

  it('switches to overview tab', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    const buttons = wrapper.findAll('button')
    const overviewButton = buttons.find(b => b.text().includes('Обзор'))
    
    if (overviewButton) {
      await overviewButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.activeTab).toBe('overview')
    }
  })

  it('switches to messages tab', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    const buttons = wrapper.findAll('button')
    const messagesButton = buttons.find(b => b.text().includes('Сообщения'))
    
    if (messagesButton) {
      await messagesButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.activeTab).toBe('messages')
    }
  })

  it('switches to config tab', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    const buttons = wrapper.findAll('button')
    const configButton = buttons.find(b => b.text().includes('Конфигурация'))
    
    if (configButton) {
      await configButton.trigger('click')
      await wrapper.vm.$nextTick()
      expect(wrapper.vm.activeTab).toBe('config')
    }
  })

  it('has activeTab ref with default value', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Dashboard</div>' } }]
    })
    
    const wrapper = mount(DashboardView, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.vm.activeTab).toBe('overview')
  })
})
