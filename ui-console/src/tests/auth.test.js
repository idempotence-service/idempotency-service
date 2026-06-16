import { describe, it, expect, beforeEach, afterEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAuthStore } from '../stores/auth.js'

describe('Auth Store', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  it('store can be created', () => {
    const store = useAuthStore()
    expect(store).toBeDefined()
  })

  it('token is empty by default', () => {
    const store = useAuthStore()
    expect(store.token).toBe('')
  })

  it('isAuthenticated is false when token is empty', () => {
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(false)
  })

  it('login sets token', () => {
    const store = useAuthStore()
    store.login('test-token')
    expect(store.token).toBe('test-token')
  })

  it('login saves token to localStorage', () => {
    const store = useAuthStore()
    store.login('test-token')
    expect(localStorage.getItem('auth_token')).toBe('test-token')
  })

  it('login makes isAuthenticated true', () => {
    const store = useAuthStore()
    store.login('test-token')
    expect(store.isAuthenticated).toBe(true)
  })

  it('logout clears token', () => {
    const store = useAuthStore()
    store.login('test-token')
    store.logout()
    expect(store.token).toBe('')
  })

  it('logout removes token from localStorage', () => {
    const store = useAuthStore()
    store.login('test-token')
    store.logout()
    expect(localStorage.getItem('auth_token')).toBe(null)
  })

  it('logout makes isAuthenticated false', () => {
    const store = useAuthStore()
    store.login('test-token')
    store.logout()
    expect(store.isAuthenticated).toBe(false)
  })

  it('initializes token from localStorage', () => {
    localStorage.setItem('auth_token', 'existing-token')
    const store = useAuthStore()
    expect(store.token).toBe('existing-token')
  })

  it('isAuthenticated is true when token exists in localStorage', () => {
    localStorage.setItem('auth_token', 'existing-token')
    const store = useAuthStore()
    expect(store.isAuthenticated).toBe(true)
  })

  it('handles empty string token in localStorage', () => {
    localStorage.setItem('auth_token', '')
    const store = useAuthStore()
    expect(store.token).toBe('')
    expect(store.isAuthenticated).toBe(false)
  })

  it('handles null token in localStorage', () => {
    localStorage.setItem('auth_token', 'null')
    const store = useAuthStore()
    expect(store.token).toBe('null')
  })

  it('login with empty string token', () => {
    const store = useAuthStore()
    store.login('')
    expect(store.token).toBe('')
    expect(store.isAuthenticated).toBe(false)
  })

  it('login with whitespace token', () => {
    const store = useAuthStore()
    store.login('   ')
    expect(store.token).toBe('   ')
    expect(store.isAuthenticated).toBe(true)
  })
})
