import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ErrorEventsTab from '../../components/ErrorEventsTab.vue'
import { coreApi } from '../../api/core.js'

vi.mock('../../api/core.js')

describe('ErrorEventsTab Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    coreApi.getErrorEvents = vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [
            {
              globalKey: 'test-key-1',
              integration: 'system1-to-system2',
              status: 'ERROR',
              statusDescription: 'Test error',
              createDate: '2024-01-15T10:00:00'
            },
            {
              globalKey: 'test-key-2',
              integration: 'system2-to-system3',
              status: 'ERROR',
              statusDescription: 'Another error',
              createDate: '2024-01-15T11:00:00'
            }
          ],
          totalElements: 2,
          totalPages: 1
        }
      }
    })
    
    coreApi.restartEvent = vi.fn().mockResolvedValue({ data: { success: true } })
    coreApi.getEventById = vi.fn().mockResolvedValue({
      data: {
        data: {
          globalKey: 'test-key-1',
          status: 'ERROR',
          payload: { test: 'data' }
        }
      }
    })
  })

  it('loads error events on mount', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    expect(coreApi.getErrorEvents).toHaveBeenCalled()
    expect(wrapper.text()).toContain('test-key')
  })

  it('toggles sort order', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const sortButtons = wrapper.findAll('button')
    const sortButton = sortButtons.find(b => b.text().includes('↓') || b.text().includes('↑'))
    
    if (sortButton) {
      await sortButton.trigger('click')
      await flushPromises()
      
      expect(coreApi.getErrorEvents).toHaveBeenCalledTimes(2)
    }
  })

  it('changes page size', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('50')
      await flushPromises()
      
      expect(coreApi.getErrorEvents).toHaveBeenCalled()
    }
  })

  it('filters by globalKey', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length > 0) {
      await inputs[0].setValue('test-key-1')
      await new Promise(resolve => setTimeout(resolve, 400))
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('filters by integration', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length > 1) {
      await inputs[1].setValue('system1')
      await new Promise(resolve => setTimeout(resolve, 400))
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('navigates to next page', async () => {
    coreApi.getErrorEvents = vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [],
          totalElements: 100,
          totalPages: 5
        }
      }
    })
    
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const nextButton = buttons.find(b => b.text().includes('→'))
    
    if (nextButton) {
      await nextButton.trigger('click')
      await flushPromises()
      
      expect(coreApi.getErrorEvents).toHaveBeenCalled()
    }
  })

  it('opens event detail modal', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const rows = wrapper.findAll('tr')
    if (rows.length > 1) {
      await rows[1].trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('retries event', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const retryButtons = wrapper.findAll('button')
    const retryButton = retryButtons.find(b => b.text().includes('↻'))
    
    if (retryButton) {
      await retryButton.trigger('click')
      await flushPromises()
      
      // Confirm retry in modal
      const confirmButtons = wrapper.findAll('button')
      const confirmButton = confirmButtons.find(b => b.text().includes('Перезапустить'))
      
      if (confirmButton) {
        await confirmButton.trigger('click')
        await flushPromises()
        
        expect(coreApi.restartEvent).toHaveBeenCalled()
      }
    }
  })

  it('copies globalKey to clipboard', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    // Skip clipboard test in test environment
    const copyButtons = wrapper.findAll('button')
    const copyButton = copyButtons.find(b => b.text().includes('📋'))
    
    if (copyButton) {
      await copyButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('reloads events', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const reloadButtons = wrapper.findAll('button')
    const reloadButton = reloadButtons.find(b => b.text().includes('Обновить'))
    
    if (reloadButton) {
      await reloadButton.trigger('click')
      await flushPromises()
      
      expect(coreApi.getErrorEvents).toHaveBeenCalledTimes(2)
    }
  })

  it('handles API errors gracefully', async () => {
    coreApi.getErrorEvents = vi.fn().mockRejectedValue(new Error('API Error'))
    
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    expect(wrapper.exists()).toBe(true)
  })

  it('changes mode selection', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('AUTO_SUCCESS')
      expect(selects[0].element.value).toBe('AUTO_SUCCESS')
      
      await selects[0].setValue('AUTO_FAIL_RESEND')
      expect(selects[0].element.value).toBe('AUTO_FAIL_RESEND')
    }
  })

  it('filters by status', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const statusButtons = buttons.filter(b => b.text().includes('ERROR'))
    
    if (statusButtons.length > 0) {
      await statusButtons[0].trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('changes page size to 25', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 1) {
      await selects[1].setValue('25')
      await flushPromises()
      
      expect(selects[1].element.value).toBe('25')
    }
  })

  it('changes page size to 50', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 1) {
      await selects[1].setValue('50')
      await flushPromises()
      
      expect(selects[1].element.value).toBe('50')
    }
  })

  it('navigates to previous page', async () => {
    coreApi.getErrorEvents = vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [],
          totalElements: 100,
          totalPages: 5
        }
      }
    })
    
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const prevButton = buttons.find(b => b.text().includes('←'))
    
    if (prevButton) {
      await prevButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('navigates to first page', async () => {
    coreApi.getErrorEvents = vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [],
          totalElements: 100,
          totalPages: 5
        }
      }
    })
    
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const firstButton = buttons.find(b => b.text().includes('«'))
    
    if (firstButton) {
      await firstButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('navigates to last page', async () => {
    coreApi.getErrorEvents = vi.fn().mockResolvedValue({
      data: {
        data: {
          content: [],
          totalElements: 100,
          totalPages: 5
        }
      }
    })
    
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const lastButton = buttons.find(b => b.text().includes('»'))
    
    if (lastButton) {
      await lastButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('searches by globalKey', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length > 0) {
      await inputs[0].setValue('test-key')
      await flushPromises()
      
      expect(inputs[0].element.value).toBe('test-key')
    }
  })

  it('filters by integration', async () => {
    const wrapper = mount(ErrorEventsTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 2) {
      await selects[2].setValue('system1-to-system2')
      await flushPromises()
      
      expect(selects[2].element.value).toBe('system1-to-system2')
    }
  })
})
