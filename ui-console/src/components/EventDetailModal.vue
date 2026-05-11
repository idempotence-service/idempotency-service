<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="modelValue"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="$emit('update:modelValue', false)"
      >
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" />
        <div class="relative w-full max-w-2xl max-h-[90vh] flex flex-col card shadow-2xl">
          <!-- Header -->
          <div class="flex items-center justify-between px-5 py-4 border-b border-slate-800 shrink-0">
            <div>
              <h2 class="text-base font-semibold text-slate-100">Детали события</h2>
              <p class="text-xs text-slate-500 mono mt-0.5 truncate max-w-xs">{{ event?.globalKey }}</p>
            </div>
            <div class="flex items-center gap-2">
              <button
                v-if="canRetry"
                @click="$emit('retry', event.globalKey)"
                class="btn-danger btn-sm"
                :disabled="retrying"
              >
                <svg v-if="retrying" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                  <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                </svg>
                <span>{{ retrying ? 'Запуск...' : '↻ Перезапустить' }}</span>
              </button>
              <button
                @click="$emit('update:modelValue', false)"
                class="w-8 h-8 flex items-center justify-center rounded-md text-slate-400 hover:text-slate-200 hover:bg-slate-800 transition-colors"
              >✕</button>
            </div>
          </div>

          <!-- Content -->
          <div v-if="loading" class="flex items-center justify-center py-16 text-slate-500">
            <svg class="w-6 h-6 animate-spin mr-2" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            Загрузка деталей...
          </div>

          <div v-else-if="details" class="overflow-y-auto p-5 space-y-4">
            <!-- Main fields -->
            <div class="grid grid-cols-2 gap-3">
              <div class="detail-field">
                <span class="detail-label">Статус</span>
                <StatusBadge :status="details.status" />
              </div>
              <div class="detail-field">
                <span class="detail-label">Интеграция</span>
                <span class="detail-value">{{ details.integration }}</span>
              </div>
              <div class="detail-field">
                <span class="detail-label">Сервис</span>
                <span class="detail-value">{{ details.service }}</span>
              </div>
              <div class="detail-field">
                <span class="detail-label">Source UID</span>
                <span class="detail-value mono text-xs">{{ details.sourceUid }}</span>
              </div>
              <div class="detail-field">
                <span class="detail-label">Создан</span>
                <span class="detail-value">{{ formatDate(details.createDate) }}</span>
              </div>
              <div class="detail-field">
                <span class="detail-label">Обновлён</span>
                <span class="detail-value">{{ formatDate(details.updateDate) }}</span>
              </div>
            </div>

            <div v-if="details.statusDescription" class="detail-field col-span-2">
              <span class="detail-label">Описание статуса</span>
              <p class="detail-value text-yellow-300">{{ details.statusDescription }}</p>
            </div>

            <div v-if="details.payload" class="space-y-1">
              <span class="detail-label">Payload</span>
              <pre class="json-block">{{ formatJson(details.payload) }}</pre>
            </div>

            <div v-if="details.headers && Object.keys(details.headers).length" class="space-y-1">
              <span class="detail-label">Headers</span>
              <pre class="json-block">{{ formatJson(details.headers) }}</pre>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<script setup>
import { ref, watch } from 'vue'
import { coreApi } from '../api/core.js'
import StatusBadge from './StatusBadge.vue'

const props = defineProps({
  modelValue: Boolean,
  event: Object,
  retrying: Boolean,
})

defineEmits(['update:modelValue', 'retry'])

const loading = ref(false)
const details = ref(null)

const canRetry = ['ERROR', 'FAILED'].includes(props.event?.status)

watch(
  () => props.modelValue,
  async (open) => {
    if (open && props.event?.globalKey) {
      loading.value = true
      details.value = null
      try {
        const res = await coreApi.getEventById(props.event.globalKey)
        details.value = res.data?.data
      } finally {
        loading.value = false
      }
    }
  }
)

function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('ru-RU', { dateStyle: 'short', timeStyle: 'medium' })
}

function formatJson(obj) {
  return JSON.stringify(obj, null, 2)
}
</script>

<style scoped>
.detail-field {
  @apply bg-slate-800/50 rounded-md px-3 py-2.5;
}
.detail-label {
  @apply block text-[10px] font-medium text-slate-500 uppercase tracking-wider mb-1;
}
.detail-value {
  @apply text-sm text-slate-200;
}
.json-block {
  @apply bg-slate-800/70 border border-slate-700 rounded-md p-3 text-xs text-slate-300 overflow-x-auto;
  font-family: 'JetBrains Mono', monospace;
}
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}
.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}
</style>
