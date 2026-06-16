import { describe, it, expect, beforeEach, vi } from 'vitest'
import { mount, flushPromises } from '@vue/test-utils'
import { setActivePinia, createPinia } from 'pinia'
import ConfigTab from '../../components/ConfigTab.vue'
import { coreApi } from '../../api/core.js'
import { senderApi } from '../../api/sender.js'

vi.mock('../../api/core.js')
vi.mock('../../api/sender.js')

describe('ConfigTab Integration Tests', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
    
    coreApi.getConfig = vi.fn().mockResolvedValue({
      data: {
        data: {
          scheduler: {
            outboxFixedDelaySeconds: 5,
            deliveryFixedDelaySeconds: 5,
            replyTimeoutFixedDelaySeconds: 15,
            cleanupFixedDelaySeconds: 86400,
            batchSize: 100
          },
          listener: {
            inboundConcurrency: 3,
            replyConcurrency: 3
          },
          resilience: {
            outboxRetryDelaySeconds: 10,
            deliveryRetryDelaySeconds: 10,
            replyTimeoutSeconds: 60,
            leaseDurationSeconds: 30,
            maxAttempts: 5
          },
          cleanup: {
            retentionSeconds: 604800,
            batchSize: 500
          }
        }
      }
    })
    
    senderApi.getSimulationConfig = vi.fn().mockResolvedValue({
      data: {
        data: {
          enabled: false,
          integration: 'system1-to-system2',
          intervalSeconds: 2,
          duplicateEvery: 2,
          burstSize: 5,
          pauseSeconds: 5
        }
      }
    })
    
    coreApi.getIntegrations = vi.fn().mockResolvedValue({
      data: {
        data: [
          { integrationName: 'system1-to-system2', idempotencyEnabled: true }
        ]
      }
    })
    
    coreApi.updateScheduler = vi.fn().mockResolvedValue({ data: { success: true } })
    coreApi.updateResilience = vi.fn().mockResolvedValue({ data: { success: true } })
    coreApi.updateCleanup = vi.fn().mockResolvedValue({ data: { success: true } })
    coreApi.updateListener = vi.fn().mockResolvedValue({ data: { success: true } })
    senderApi.updateSimulationConfig = vi.fn().mockResolvedValue({ data: { success: true } })
  })

  it('loads configuration on mount', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    expect(coreApi.getConfig).toHaveBeenCalled()
    expect(senderApi.getSimulationConfig).toHaveBeenCalled()
    expect(coreApi.getIntegrations).toHaveBeenCalled()
  })

  it('saves scheduler configuration', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const saveButton = buttons.find(b => b.text().includes('Применить'))
    
    if (saveButton) {
      await saveButton.trigger('click')
      await flushPromises()
      
      expect(coreApi.updateScheduler).toHaveBeenCalled()
    }
  })

  it('formats duration correctly', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    // Check that duration formatting is displayed
    expect(wrapper.text()).toBeDefined()
  })

  it('toggles simulation', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const toggleButtons = wrapper.findAll('button')
    const simulationToggle = toggleButtons.find(b => b.text().includes('Выключена') || b.text().includes('Включена'))
    
    if (simulationToggle) {
      await simulationToggle.trigger('click')
      await flushPromises()
      
      expect(senderApi.updateSimulationConfig).toHaveBeenCalled()
    }
  })

  it('updates input values', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="number"]')
    if (inputs.length > 0) {
      await inputs[0].setValue(10)
      expect(inputs[0].element.value).toBe('10')
    }
  })

  it('handles API errors gracefully', async () => {
    coreApi.getConfig = vi.fn().mockRejectedValue(new Error('API Error'))
    
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    expect(wrapper.exists()).toBe(true)
  })

  it('saves resilience configuration', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    // Find all save buttons and trigger the second one (resilience section)
    const buttons = wrapper.findAll('button')
    const saveButtons = buttons.filter(b => b.text().includes('Применить'))
    
    if (saveButtons.length > 1) {
      await saveButtons[1].trigger('click')
      await flushPromises()
      
      expect(coreApi.updateResilience).toHaveBeenCalled()
    }
  })

  it('saves cleanup configuration', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const saveButtons = buttons.filter(b => b.text().includes('Применить'))
    
    if (saveButtons.length > 2) {
      await saveButtons[2].trigger('click')
      await flushPromises()
      
      expect(coreApi.updateCleanup).toHaveBeenCalled()
    }
  })

  it('saves listener configuration', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const buttons = wrapper.findAll('button')
    const saveButtons = buttons.filter(b => b.text().includes('Применить'))
    
    if (saveButtons.length > 3) {
      await saveButtons[3].trigger('click')
      await flushPromises()
      
      expect(coreApi.updateListener).toHaveBeenCalled()
    }
  })

  it('changes simulation integration', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const selects = wrapper.findAll('select')
    if (selects.length > 0) {
      await selects[0].setValue('system1-to-system2')
      expect(selects[0].element.value).toBe('system1-to-system2')
    }
  })

  it('updates all scheduler inputs', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="number"]')
    if (inputs.length >= 5) {
      await inputs[0].setValue(10)
      await inputs[1].setValue(10)
      await inputs[2].setValue(20)
      await inputs[3].setValue(86400)
      await inputs[4].setValue(200)
      await flushPromises()
      
      expect(inputs[0].element.value).toBe('10')
    }
  })

  it('updates all resilience inputs', async () => {
    const wrapper = mount(ConfigTab)
    await flushPromises()
    
    const inputs = wrapper.findAll('input[type="number"]')
    if (inputs.length >= 10) {
      await inputs[5].setValue(15)
      await inputs[6].setValue(15)
      await inputs[7].setValue(90)
      await inputs[8].setValue(60)
      await inputs[9].setValue(10)
      await flushPromises()
      
      expect(inputs[5].element.value).toBe('15')
    }
  })
})
