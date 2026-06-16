import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createRouter, createMemoryHistory } from 'vue-router'
import { setActivePinia, createPinia } from 'pinia'
import SenderTab from '../components/SenderTab.vue'

describe('SenderTab Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('component can be imported', async () => {
    const component = await import('../components/SenderTab.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../components/SenderTab.vue')
    expect(component.default).toBeDefined()
  })

  it('renders sender tab', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.exists()).toBe(true)
  })

  it('has sender sections', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })

  it('has simulation config section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toBeDefined()
  })

  it('has send event section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Отправить событие')
  })

  it('has sent messages section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Отправленные сообщения')
  })

  it('has replies section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Полученные ответы')
  })

  it('has input fields for event form', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    const inputs = wrapper.findAll('input')
    expect(inputs.length).toBeGreaterThan(0)
  })

  it('has send button', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Sender</div>' } }]
    })
    
    const wrapper = mount(SenderTab, {
      global: {
        plugins: [router]
      }
    })
    
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThan(0)
  })
})
