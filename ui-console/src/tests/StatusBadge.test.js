import { describe, it, expect } from 'vitest'
import { mount } from '@vue/test-utils'
import StatusBadge from '../components/StatusBadge.vue'

describe('StatusBadge', () => {
  it('renders status text', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'ERROR' } })
    expect(wrapper.text()).toBe('ERROR')
  })

  it('applies error class for error statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'ERROR' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-error')
  })

  it('applies error class for fail statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'FAILED' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-error')
  })

  it('applies success class for success statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'SUCCESS' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-success')
  })

  it('applies success class for complete statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'COMPLETE' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-success')
  })

  it('applies warning class for waiting statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'WAITING' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-warning')
  })

  it('applies info class for retry statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'RETRY' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-info')
  })

  it('applies default class for unknown statuses', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'UNKNOWN' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-default')
  })

  it('handles case-insensitive status matching', () => {
    const wrapper = mount(StatusBadge, { props: { status: 'error' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-error')
  })

  it('handles empty status', () => {
    const wrapper = mount(StatusBadge, { props: { status: '' } })
    expect(wrapper.find('.badge').classes()).toContain('badge-default')
  })
})
