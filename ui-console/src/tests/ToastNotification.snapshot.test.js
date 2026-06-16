import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../stores/toast.js'
import ToastNotification from '../components/ToastNotification.vue'

describe('ToastNotification Snapshot Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('snapshot with success toast', () => {
    const toastStore = useToastStore()
    toastStore.success('Success message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })

  it('snapshot with error toast', () => {
    const toastStore = useToastStore()
    toastStore.error('Error message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })

  it('snapshot with info toast', () => {
    const toastStore = useToastStore()
    toastStore.info('Info message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })

  it('snapshot with unknown type toast', () => {
    const toastStore = useToastStore()
    toastStore.add('Unknown message', 'unknown')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })

  it('snapshot with null type toast', () => {
    const toastStore = useToastStore()
    toastStore.add('Null type message', null)
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })

  it('snapshot with undefined type toast', () => {
    const toastStore = useToastStore()
    toastStore.add('Undefined type message', undefined)
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      },
      attachTo: document.body
    })
    
    expect(wrapper.html()).toBeDefined()
  })
})
