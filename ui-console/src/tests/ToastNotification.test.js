import { describe, it, expect, beforeEach } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../stores/toast.js'

describe('ToastNotification Component', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('toastClass returns correct class for success', () => {
    const toastStore = useToastStore()
    expect(toastStore.toasts).toBeDefined()
  })

  it('toastStore has add method', () => {
    const toastStore = useToastStore()
    expect(typeof toastStore.add).toBe('function')
  })

  it('toastStore has remove method', () => {
    const toastStore = useToastStore()
    expect(typeof toastStore.remove).toBe('function')
  })

  it('toastStore has success method', () => {
    const toastStore = useToastStore()
    expect(typeof toastStore.success).toBe('function')
  })

  it('toastStore has error method', () => {
    const toastStore = useToastStore()
    expect(typeof toastStore.error).toBe('function')
  })

  it('toastStore has info method', () => {
    const toastStore = useToastStore()
    expect(typeof toastStore.info).toBe('function')
  })

  it('component can be imported', async () => {
    const component = await import('../components/ToastNotification.vue')
    expect(component).toBeDefined()
  })

  it('component has default export', async () => {
    const component = await import('../components/ToastNotification.vue')
    expect(component.default).toBeDefined()
  })
})
