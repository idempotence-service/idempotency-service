<template>
  <Teleport to="body">
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-2 w-80">
      <TransitionGroup name="toast">
        <div
          v-for="toast in toastStore.toasts"
          :key="toast.id"
          class="flex items-start gap-3 px-4 py-3 rounded-lg shadow-lg border text-sm cursor-pointer"
          :class="toastClass(toast.type)"
          @click="toastStore.remove(toast.id)"
        >
          <span class="text-lg leading-none mt-0.5">{{ toastIcon(toast.type) }}</span>
          <span class="flex-1 leading-snug">{{ toast.message }}</span>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<script setup>
import { useToastStore } from '../stores/toast.js'

const toastStore = useToastStore()

function toastClass(type) {
  return {
    success: 'bg-emerald-900/90 border-emerald-700 text-emerald-100',
    error: 'bg-red-900/90 border-red-700 text-red-100',
    info: 'bg-slate-800/90 border-slate-700 text-slate-100',
  }[type] ?? 'bg-slate-800/90 border-slate-700 text-slate-100'
}

function toastIcon(type) {
  return { success: '✓', error: '✕', info: 'ℹ' }[type] ?? 'ℹ'
}
</script>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s ease;
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(100%);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(100%);
}
</style>
