import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useToastStore } from '../stores/toast'

describe('Toast Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
  })

  it('initializes with empty toasts', () => {
    const toast = useToastStore()
    expect(toast.toasts).toEqual([])
  })

  it('adds a success toast', () => {
    const toast = useToastStore()
    toast.success('Success message')
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].message).toBe('Success message')
    expect(toast.toasts[0].type).toBe('success')
  })

  it('adds an error toast', () => {
    const toast = useToastStore()
    toast.error('Error message')
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].message).toBe('Error message')
    expect(toast.toasts[0].type).toBe('error')
  })

  it('adds an info toast', () => {
    const toast = useToastStore()
    toast.info('Info message')
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].message).toBe('Info message')
    expect(toast.toasts[0].type).toBe('info')
  })

  it('removes a toast by id', () => {
    const toast = useToastStore()
    toast.success('Test')
    const id = toast.toasts[0].id
    toast.remove(id)
    expect(toast.toasts).toHaveLength(0)
  })

  it('generates unique ids for toasts', () => {
    const toast = useToastStore()
    toast.success('Test 1')
    toast.success('Test 2')
    expect(toast.toasts[0].id).not.toBe(toast.toasts[1].id)
  })

  it('can add toast with custom type', () => {
    const toast = useToastStore()
    toast.add('Custom message', 'warning')
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].type).toBe('warning')
  })

  it('can add multiple toasts', () => {
    const toast = useToastStore()
    toast.success('First')
    toast.error('Second')
    toast.info('Third')
    expect(toast.toasts).toHaveLength(3)
  })

  it('removes correct toast when multiple exist', () => {
    const toast = useToastStore()
    toast.success('First')
    toast.error('Second')
    const idToRemove = toast.toasts[0].id
    toast.remove(idToRemove)
    expect(toast.toasts).toHaveLength(1)
    expect(toast.toasts[0].message).toBe('Second')
  })

  it('does not remove toast with wrong id', () => {
    const toast = useToastStore()
    toast.success('Test')
    toast.remove('wrong-id')
    expect(toast.toasts).toHaveLength(1)
  })

  it('add function uses default type parameter', () => {
    const toast = useToastStore()
    toast.add('Test message')
    expect(toast.toasts[0].type).toBe('info')
  })

  it('add function uses custom type parameter', () => {
    const toast = useToastStore()
    toast.add('Test message', 'warning')
    expect(toast.toasts[0].type).toBe('warning')
  })

  it('add function uses default duration parameter', () => {
    const toast = useToastStore()
    toast.add('Test message')
    expect(toast.toasts).toHaveLength(1)
  })

  it('add function uses custom duration parameter', () => {
    const toast = useToastStore()
    toast.add('Test message', 'info', 1000)
    expect(toast.toasts).toHaveLength(1)
  })
})
