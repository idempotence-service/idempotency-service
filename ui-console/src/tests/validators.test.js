import { describe, it, expect } from 'vitest'

describe('Validator Functions', () => {
  describe('email validation', () => {
    it('validates correct email', () => {
      const email = 'test@example.com'
      const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      expect(isValid).toBe(true)
    })

    it('invalidates email without @', () => {
      const email = 'testexample.com'
      const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      expect(isValid).toBe(false)
    })

    it('invalidates email without domain', () => {
      const email = 'test@'
      const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      expect(isValid).toBe(false)
    })

    it('invalidates empty email', () => {
      const email = ''
      const isValid = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)
      expect(isValid).toBe(false)
    })

    it('invalidates null email', () => {
      const email = null
      const isValid = email ? /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email) : false
      expect(isValid).toBe(false)
    })
  })

  describe('URL validation', () => {
    it('validates correct URL', () => {
      const url = 'https://example.com'
      const isValid = /^https?:\/\/.+\..+/.test(url)
      expect(isValid).toBe(true)
    })

    it('validates HTTP URL', () => {
      const url = 'http://example.com'
      const isValid = /^https?:\/\/.+\..+/.test(url)
      expect(isValid).toBe(true)
    })

    it('invalidates URL without protocol', () => {
      const url = 'example.com'
      const isValid = /^https?:\/\/.+\..+/.test(url)
      expect(isValid).toBe(false)
    })

    it('invalidates empty URL', () => {
      const url = ''
      const isValid = /^https?:\/\/.+\..+/.test(url)
      expect(isValid).toBe(false)
    })
  })

  describe('number range validation', () => {
    it('validates number in range', () => {
      const num = 50
      const min = 0
      const max = 100
      const isValid = num >= min && num <= max
      expect(isValid).toBe(true)
    })

    it('invalidates number below range', () => {
      const num = -10
      const min = 0
      const max = 100
      const isValid = num >= min && num <= max
      expect(isValid).toBe(false)
    })

    it('invalidates number above range', () => {
      const num = 150
      const min = 0
      const max = 100
      const isValid = num >= min && num <= max
      expect(isValid).toBe(false)
    })

    it('validates at lower boundary', () => {
      const num = 0
      const min = 0
      const max = 100
      const isValid = num >= min && num <= max
      expect(isValid).toBe(true)
    })

    it('validates at upper boundary', () => {
      const num = 100
      const min = 0
      const max = 100
      const isValid = num >= min && num <= max
      expect(isValid).toBe(true)
    })
  })

  describe('string length validation', () => {
    it('validates string within length', () => {
      const str = 'test'
      const minLength = 1
      const maxLength = 10
      const isValid = str.length >= minLength && str.length <= maxLength
      expect(isValid).toBe(true)
    })

    it('invalidates string too short', () => {
      const str = ''
      const minLength = 1
      const maxLength = 10
      const isValid = str.length >= minLength && str.length <= maxLength
      expect(isValid).toBe(false)
    })

    it('invalidates string too long', () => {
      const str = 'very-long-string'
      const minLength = 1
      const maxLength = 10
      const isValid = str.length >= minLength && str.length <= maxLength
      expect(isValid).toBe(false)
    })

    it('validates at minimum length', () => {
      const str = 'a'
      const minLength = 1
      const maxLength = 10
      const isValid = str.length >= minLength && str.length <= maxLength
      expect(isValid).toBe(true)
    })

    it('validates at maximum length', () => {
      const str = '1234567890'
      const minLength = 1
      const maxLength = 10
      const isValid = str.length >= minLength && str.length <= maxLength
      expect(isValid).toBe(true)
    })
  })

  describe('required field validation', () => {
    it('validates non-empty string', () => {
      const value = 'test'
      const isValid = value && value.trim().length > 0
      expect(isValid).toBe(true)
    })

    it('invalidates empty string', () => {
      const value = ''
      const isValid = value && value.trim().length > 0
      expect(isValid).toBeFalsy()
    })

    it('invalidates whitespace-only string', () => {
      const value = '   '
      const isValid = value && value.trim().length > 0
      expect(isValid).toBeFalsy()
    })

    it('invalidates null value', () => {
      const value = null
      const isValid = value && value.trim().length > 0
      expect(isValid).toBeFalsy()
    })

    it('invalidates undefined value', () => {
      const value = undefined
      const isValid = value && value.trim().length > 0
      expect(isValid).toBeFalsy()
    })
  })
})
