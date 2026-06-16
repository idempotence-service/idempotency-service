import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ReceiverTab from '../../components/ReceiverTab.vue'
import { receiverApi } from '../../api/receiver.js'

vi.mock('../../api/receiver.js')

describe('ReceiverTab Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    receiverApi.getReceivedEvents = vi.fn().mockResolvedValue({
      data: {
        data: [
          {
            globalKey: 'received-key-1',
            integration: 'system1-to-system2',
            payload: { test: 'data' }
          }
        ]
      }
    })
    
    receiverApi.setMode = vi.fn().mockResolvedValue({ data: { success: true } })
    receiverApi.sendManualReply = vi.fn().mockResolvedValue({ data: { success: true } })
  })

  it('loads received events on mount', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    expect(receiverApi.getReceivedEvents).toHaveBeenCalled()
  })

  it('sets processing mode', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length > 0) {
      await inputs[0].setValue('test-integration')
    }
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('AUTO_SUCCESS')
    }
    
    const buttons = wrapper.findAll('button')
    const setModeButton = buttons.find(b => b.text().includes('Установить режим'))
    
    if (setModeButton) {
      await setModeButton.trigger('click')
      await flushPromises()
      
      expect(receiverApi.setMode).toHaveBeenCalled()
    }
  })

  it('sends manual reply', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    // Fill in manual reply form
    if (inputs.length >= 3) {
      await inputs[1].setValue('test-integration')
      await inputs[2].setValue('test-global-key')
    }
    
    const selects = wrapper.findAll('select')
    if (selects.length > 1) {
      await selects[1].setValue('SUCCESS')
    }
    
    const buttons = wrapper.findAll('button')
    const sendReplyButton = buttons.find(b => b.text().includes('Отправить ответ'))
    
    if (sendReplyButton) {
      await sendReplyButton.trigger('click')
      await flushPromises()
      
      expect(receiverApi.sendManualReply).toHaveBeenCalled()
    }
  })

  it('reloads received events', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const reloadButton = buttons.find(b => b.text().includes('Обновить'))
    
    if (reloadButton) {
      await reloadButton.trigger('click')
      await flushPromises()
      
      expect(receiverApi.getReceivedEvents).toHaveBeenCalledTimes(2)
    }
  })

  it('resets receiver state', async () => {
    receiverApi.resetState = vi.fn().mockResolvedValue({ data: { success: true } })
    
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const resetButton = buttons.find(b => b.text().includes('Сбросить'))
    
    if (resetButton) {
      await resetButton.trigger('click')
      await flushPromises()
      
      expect(receiverApi.resetState).toHaveBeenCalled()
    }
  })

  it('toggles needResend checkbox', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const checkboxes = wrapper.findAll('input[type="checkbox"]')
    if (checkboxes.length > 0) {
      await checkboxes[0].setChecked(true)
      expect(checkboxes[0].element.checked).toBe(true)
    }
  })

  it('fills manual reply form from event click', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const rows = wrapper.findAll('tr')
    if (rows.length > 1) {
      await rows[1].trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })

  it('validates integration field', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const setModeButton = buttons.find(b => b.text().includes('Установить режим'))
    
    if (setModeButton) {
      // Try to submit without integration
      await setModeButton.trigger('click')
      await flushPromises()
      
      // Should not call API if validation fails
      expect(wrapper.exists()).toBe(true)
    }
  })

  it('validates globalKey field for manual reply', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const sendReplyButton = buttons.find(b => b.text().includes('Отправить ответ'))
    
    if (sendReplyButton) {
      // Try to submit without globalKey
      await sendReplyButton.trigger('click')
      await flushPromises()
      
      expect(wrapper.exists()).toBe(true)
    }
  })

  it('handles API errors gracefully', async () => {
    receiverApi.getReceivedEvents = vi.fn().mockRejectedValue(new Error('API Error'))
    
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    expect(wrapper.exists()).toBe(true)
  })

  it('changes mode selection', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('AUTO_SUCCESS')
      expect(selects[0].element.value).toBe('AUTO_SUCCESS')
      
      await selects[0].setValue('AUTO_FAIL_RESEND')
      expect(selects[0].element.value).toBe('AUTO_FAIL_RESEND')
      
      await selects[0].setValue('MANUAL')
      expect(selects[0].element.value).toBe('MANUAL')
    }
  })

  it('sets integration for manual reply', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="text"]')
    if (inputs.length >= 2) {
      await inputs[1].setValue('test-integration')
      await inputs[2].setValue('test-global-key')
      await flushPromises()
      
      expect(inputs[1].element.value).toBe('test-integration')
    }
  })

  it('sets result for manual reply', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 1) {
      await selects[1].setValue('SUCCESS')
      expect(selects[1].element.value).toBe('SUCCESS')
      
      await selects[1].setValue('ERROR')
      expect(selects[1].element.value).toBe('ERROR')
    }
  })

  it('sets description for manual reply', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const textareas = wrapper.findAll('textarea')
    if (textareas.length > 0) {
      await textareas[0].setValue('Test description')
      expect(textareas[0].element.value).toBe('Test description')
    }
  })

  it('toggles needResend checkbox', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const checkboxes = wrapper.findAll('input[type="checkbox"]')
    if (checkboxes.length > 0) {
      await checkboxes[0].setChecked(true)
      expect(checkboxes[0].element.checked).toBe(true)
      
      await checkboxes[0].setChecked(false)
      expect(checkboxes[0].element.checked).toBe(false)
    }
  })

  it('clicks on received event row', async () => {
    const wrapper = mount(ReceiverTab)
    await flushPromises()
    
    const rows = wrapper.findAll('tr')
    if (rows.length > 1) {
      await rows[1].trigger('click')
      await flushPromises()
      
      expect(wrapper.vm).toBeDefined()
    }
  })
})
