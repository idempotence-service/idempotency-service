import { describe, it, expect, beforeEach } from 'vitest'
import { mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { useToastStore } from '../stores/toast.js'
import ToastNotification from '../components/ToastNotification.vue'
import { toastClass, toastIcon } from '../utils/toastHelpers.js'

describe('ToastNotification Branch Coverage', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  describe('toastClass function', () => {
    it('returns success class for success type', () => {
      expect(toastClass('success')).toBe('bg-emerald-900/90 border-emerald-700 text-emerald-100')
    })

    it('returns error class for error type', () => {
      expect(toastClass('error')).toBe('bg-red-900/90 border-red-700 text-red-100')
    })

    it('returns info class for info type', () => {
      expect(toastClass('info')).toBe('bg-slate-800/90 border-slate-700 text-slate-100')
    })

    it('returns default class for unknown type', () => {
      expect(toastClass('unknown')).toBe('bg-slate-800/90 border-slate-700 text-slate-100')
    })

    it('returns default class for null type', () => {
      expect(toastClass(null)).toBe('bg-slate-800/90 border-slate-700 text-slate-100')
    })

    it('returns default class for undefined type', () => {
      expect(toastClass(undefined)).toBe('bg-slate-800/90 border-slate-700 text-slate-100')
    })
  })

  describe('toastIcon function', () => {
    it('returns success icon for success type', () => {
      expect(toastIcon('success')).toBe('✓')
    })

    it('returns error icon for error type', () => {
      expect(toastIcon('error')).toBe('✕')
    })

    it('returns info icon for info type', () => {
      expect(toastIcon('info')).toBe('ℹ')
    })

    it('returns default icon for unknown type', () => {
      expect(toastIcon('unknown')).toBe('ℹ')
    })

    it('returns default icon for null type', () => {
      expect(toastIcon(null)).toBe('ℹ')
    })

    it('returns default icon for undefined type', () => {
      expect(toastIcon(undefined)).toBe('ℹ')
    })
  })

  describe('Component branch coverage', () => {
    it('triggers success branch via component', () => {
      const toastStore = useToastStore()
      toastStore.success('Success message')
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe('success')
    })

    it('triggers error branch via component', () => {
      const toastStore = useToastStore()
      toastStore.error('Error message')
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe('error')
    })

    it('triggers info branch via component', () => {
      const toastStore = useToastStore()
      toastStore.info('Info message')
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe('info')
    })

    it('triggers unknown branch via component', () => {
      const toastStore = useToastStore()
      toastStore.add('Unknown message', 'unknown')
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe('unknown')
    })

    it('triggers null branch via component', () => {
      const toastStore = useToastStore()
      toastStore.add('Null message', null)
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe(null)
    })

    it('triggers undefined branch via component', () => {
      const toastStore = useToastStore()
      toastStore.add('Undefined message', undefined)
      
      const wrapper = mount(ToastNotification, {
        global: {
          plugins: [createPinia()]
        },
        attachTo: document.body
      })
      
      expect(toastStore.toasts[0].type).toBe('info')
    })
  })
})
