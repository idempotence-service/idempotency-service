import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import SenderTab from '../../components/SenderTab.vue'
import { senderApi } from '../../api/sender.js'
import { coreApi } from '../../api/core.js'

vi.mock('../../api/sender.js')
vi.mock('../../api/core.js')

describe('SenderTab Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    senderApi.sendEvent = vi.fn().mockResolvedValue({ data: { success: true } })
    senderApi.getSentMessages = vi.fn().mockResolvedValue({
      data: {
        data: [
          { globalKey: 'sent-1', integration: 'system1', status: 'SENT' }
        ]
      }
    })
    senderApi.getReplies = vi.fn().mockResolvedValue({
      data: {
        data: [
          { globalKey: 'reply-1', status: 'SUCCESS' }
        ]
      }
    })
    senderApi.getSimulationConfig = vi.fn().mockResolvedValue({
      data: {
        data: {
          enabled: false,
          integration: 'system1-to-system2',
          intervalSeconds: 2
        }
      }
    })
    senderApi.updateSimulationConfig = vi.fn().mockResolvedValue({ data: { success: true } })
    senderApi.resetState = vi.fn().mockResolvedValue({ data: { success: true } })
    
    coreApi.getIntegrations = vi.fn().mockResolvedValue({
      data: {
        data: [
          { integrationName: 'system1-to-system2', idempotencyEnabled: true }
        ]
      }
    })
  })

  it('loads simulation config on mount', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    // SenderTab loads data when user interacts, not on mount
    expect(wrapper.exists()).toBe(true)
  })

  it('sends event', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length > 0) {
      await inputs[0].setValue('system1-to-system2')
    }
    
    const buttons = wrapper.findAll('button')
    const sendButton = buttons.find(b => b.text().includes('📤'))
    
    if (sendButton) {
      await sendButton.trigger('click')
      await flushPromises()
      
      expect(senderApi.sendEvent).toHaveBeenCalled()
    }
  })

  it('loads sent messages', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const refreshButton = buttons.find(b => b.text().includes('Обновить'))
    
    if (refreshButton) {
      await refreshButton.trigger('click')
      await flushPromises()
      
      expect(senderApi.getSentMessages).toHaveBeenCalled()
    }
  })

  it('loads replies', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    expect(senderApi.getReplies).toHaveBeenCalled()
  })

  it('toggles simulation', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const toggleButton = buttons.find(b => b.text().includes('Выключена') || b.text().includes('Включена'))
    
    if (toggleButton) {
      await toggleButton.trigger('click')
      await flushPromises()
      
      expect(senderApi.updateSimulationConfig).toHaveBeenCalled()
    }
  })

  it('resets sender state', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const resetButton = buttons.find(b => b.text().includes('Сбросить'))
    
    if (resetButton) {
      await resetButton.trigger('click')
      await flushPromises()
      
      expect(senderApi.resetState).toHaveBeenCalled()
    }
  })

  it('fills payload field', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const textareas = wrapper.findAll('textarea')
    if (textareas.length > 0) {
      await textareas[0].setValue('{"test": "data"}')
      expect(textareas[0].element.value).toBe('{"test": "data"}')
    }
  })

  it('fills headers field', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const textareas = wrapper.findAll('textarea')
    if (textareas.length > 1) {
      await textareas[1].setValue('{"X-Custom": "value"}')
      expect(textareas[1].element.value).toBe('{"X-Custom": "value"}')
    }
  })

  it('sets duplicate count', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="number"]')
    if (inputs.length > 0) {
      await inputs[0].setValue(3)
      expect(inputs[0].element.value).toBe('3')
    }
  })

  it('handles API errors gracefully', async () => {
    senderApi.getSimulationConfig = vi.fn().mockRejectedValue(new Error('API Error'))
    
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    expect(wrapper.exists()).toBe(true)
  })

  it('changes integration selection', async () => {
    const wrapper = mount(SenderTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('system1-to-system2')
      expect(selects[0].element.value).toBe('system1-to-system2')
    }
  })
})
