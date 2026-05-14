import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useToastStore = defineStore('toast', () => {
  const toasts = ref([])
  let counter = 0

  function add(message, type = 'info', duration = 4000) {
    const id = ++counter
    toasts.value.push({ id, message, type })
    setTimeout(() => remove(id), duration)
  }

  function remove(id) {
    const idx = toasts.value.findIndex((t) => t.id === id)
    if (idx !== -1) toasts.value.splice(idx, 1)
  }

  const success = (msg) => add(msg, 'success')
  const error = (msg) => add(msg, 'error')
  const info = (msg) => add(msg, 'info')

  return { toasts, add, remove, success, error, info }
})
