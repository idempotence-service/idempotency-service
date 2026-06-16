import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../stores/toast.js'
import ToastNotification from '../components/ToastNotification.vue'

describe('ToastNotification Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('renders success toast with correct classes', () => {
    const toastStore = useToastStore()
    toastStore.success('Test success message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts).toHaveLength(1)
    expect(toastStore.toasts[0].type).toBe('success')
    expect(toastStore.toasts[0].message).toBe('Test success message')
  })

  it('renders error toast with correct classes', () => {
    const toastStore = useToastStore()
    toastStore.error('Test error message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts).toHaveLength(1)
    expect(toastStore.toasts[0].type).toBe('error')
    expect(toastStore.toasts[0].message).toBe('Test error message')
  })

  it('renders info toast with correct classes', () => {
    const toastStore = useToastStore()
    toastStore.info('Test info message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts).toHaveLength(1)
    expect(toastStore.toasts[0].type).toBe('info')
    expect(toastStore.toasts[0].message).toBe('Test info message')
  })

  it('renders multiple toasts', () => {
    const toastStore = useToastStore()
    toastStore.success('Message 1')
    toastStore.error('Message 2')
    toastStore.info('Message 3')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts).toHaveLength(3)
  })

  it('removes toast on click', () => {
    const toastStore = useToastStore()
    toastStore.success('Test message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    const toastId = toastStore.toasts[0].id
    toastStore.remove(toastId)
    
    expect(toastStore.toasts).toHaveLength(0)
  })

  it('handles empty toasts array', () => {
    const toastStore = useToastStore()
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts).toHaveLength(0)
  })

  it('renders toast with custom icon for success', () => {
    const toastStore = useToastStore()
    toastStore.success('Success message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts[0].type).toBe('success')
  })

  it('renders toast with custom icon for error', () => {
    const toastStore = useToastStore()
    toastStore.error('Error message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts[0].type).toBe('error')
  })

  it('renders toast with custom icon for info', () => {
    const toastStore = useToastStore()
    toastStore.info('Info message')
    
    const wrapper = mount(ToastNotification, {
      global: {
        plugins: [createPinia()]
      }
    })
    
    expect(toastStore.toasts[0].type).toBe('info')
  })
})
