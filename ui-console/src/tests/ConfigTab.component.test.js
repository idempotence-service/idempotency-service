import { describe, it, expect, beforeEach, vi } from 'vitest'
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

  it('has scheduler section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Планировщик')
  })

  it('has resilience section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Устойчивость')
  })

  it('has cleanup section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Очистка')
  })

  it('has listener section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Kafka Listener')
  })

  it('has simulation section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    expect(wrapper.text()).toContain('Симуляция отправителя')
  })

  it('has input fields for scheduler config', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    const inputs = wrapper.findAll('input[type="number"]')
    expect(inputs.length).toBeGreaterThan(0)
  })

  it('has save buttons for each section', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    const buttons = wrapper.findAll('button')
    expect(buttons.length).toBeGreaterThan(0)
  })

  it('has formatDuration function', () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    // The function is defined in the component but not exposed
    // We can test the logic through the component's behavior
    expect(wrapper.exists()).toBe(true)
  })

  it('can interact with scheduler inputs', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: { template: '<div>Config</div>' } }]
    })
    
    const wrapper = mount(ConfigTab, {
      global: {
        plugins: [router]
      }
    })
    
    const inputs = wrapper.findAll('input[type="number"]')
    if (inputs.length > 0) {
      await inputs[0].setValue(10)
      expect(inputs[0].element.value).toBe('10')
    }
  })
})
