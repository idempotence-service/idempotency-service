<template>
  <div class="space-y-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">Конфигурация</h2>
        <p class="text-sm mt-1" style="color:var(--md-on-surface-v)">Управление параметрами сервиса в реальном времени</p>
      </div>
      <button @click="loadAll" :disabled="loading" class="btn-tonal">
        <svg class="w-4 h-4" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        Обновить
      </button>
    </div>

    <div v-if="loading" class="flex items-center justify-center py-20 text-sm" style="color:var(--md-on-surface-v)">
      Загрузка...
    </div>

    <template v-else>

      <!-- Scheduler -->
      <div class="card p-6 space-y-5">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" style="background:rgba(208,188,255,0.12)">
            <svg class="w-4 h-4" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-sm" style="color:var(--md-on-surface)">Планировщик</h3>
            <p class="text-xs" style="color:var(--md-on-surface-v)">Интервалы фоновых задач · изменения применяются со следующего цикла</p>
          </div>
        </div>
        <div class="grid grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label class="label">Outbox delay (сек)</label>
            <input v-model.number="scheduler.outboxFixedDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Delivery delay (сек)</label>
            <input v-model.number="scheduler.deliveryFixedDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Reply timeout check (сек)</label>
            <input v-model.number="scheduler.replyTimeoutFixedDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Cleanup delay (сек)</label>
            <input v-model.number="scheduler.cleanupFixedDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Batch size</label>
            <input v-model.number="scheduler.batchSize" type="number" min="1" class="input" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="saveScheduler" :disabled="saving.scheduler" class="btn-primary btn-sm">
            {{ saving.scheduler ? 'Сохранение...' : 'Применить' }}
          </button>
        </div>
      </div>

      <!-- Resilience -->
      <div class="card p-6 space-y-5">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" style="background:rgba(109,213,140,0.12)">
            <svg class="w-4 h-4" style="color:var(--md-success)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-sm" style="color:var(--md-on-surface)">Устойчивость</h3>
            <p class="text-xs" style="color:var(--md-on-surface-v)">Повторные попытки и таймауты · применяются к новым операциям немедленно</p>
          </div>
        </div>
        <div class="grid grid-cols-2 lg:grid-cols-3 gap-4">
          <div>
            <label class="label">Outbox retry delay (сек)</label>
            <input v-model.number="resilience.outboxRetryDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Delivery retry delay (сек)</label>
            <input v-model.number="resilience.deliveryRetryDelaySeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Reply timeout (сек)</label>
            <input v-model.number="resilience.replyTimeoutSeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Lease duration (сек)</label>
            <input v-model.number="resilience.leaseDurationSeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Max attempts</label>
            <input v-model.number="resilience.maxAttempts" type="number" min="1" class="input" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="saveResilience" :disabled="saving.resilience" class="btn-primary btn-sm">
            {{ saving.resilience ? 'Сохранение...' : 'Применить' }}
          </button>
        </div>
      </div>

      <!-- Cleanup -->
      <div class="card p-6 space-y-5">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" style="background:rgba(242,184,181,0.12)">
            <svg class="w-4 h-4" style="color:var(--md-error)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-sm" style="color:var(--md-on-surface)">Очистка</h3>
            <p class="text-xs" style="color:var(--md-on-surface-v)">Удаление завершённых записей · применяется к следующему запуску cleanup</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="label">Retention (сек)</label>
            <input v-model.number="cleanup.retentionSeconds" type="number" min="1" class="input" />
            <p class="text-xs mt-1" style="color:var(--md-on-surface-v)">{{ formatDuration(cleanup.retentionSeconds) }}</p>
          </div>
          <div>
            <label class="label">Batch size</label>
            <input v-model.number="cleanup.batchSize" type="number" min="1" class="input" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="saveCleanup" :disabled="saving.cleanup" class="btn-primary btn-sm">
            {{ saving.cleanup ? 'Сохранение...' : 'Применить' }}
          </button>
        </div>
      </div>

      <!-- Listener -->
      <div class="card p-6 space-y-5">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" style="background:rgba(246,193,66,0.12)">
            <svg class="w-4 h-4" style="color:var(--md-warning)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h7"/>
            </svg>
          </div>
          <div>
            <h3 class="font-semibold text-sm" style="color:var(--md-on-surface)">Kafka Listener</h3>
            <p class="text-xs" style="color:var(--md-on-surface-v)">Значение сохраняется, но вступает в силу только после перезапуска сервиса</p>
          </div>
        </div>
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="label">Inbound concurrency</label>
            <input v-model.number="listener.inboundConcurrency" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Reply concurrency</label>
            <input v-model.number="listener.replyConcurrency" type="number" min="1" class="input" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="saveListener" :disabled="saving.listener" class="btn-primary btn-sm">
            {{ saving.listener ? 'Сохранение...' : 'Применить' }}
          </button>
        </div>
      </div>

      <!-- Simulation -->
      <div class="card p-6 space-y-5">
        <div class="flex items-center gap-3">
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" style="background:rgba(208,188,255,0.12)">
            <svg class="w-4 h-4" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z"/>
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z"/>
            </svg>
          </div>
          <div class="flex-1">
            <h3 class="font-semibold text-sm" style="color:var(--md-on-surface)">Симуляция отправителя</h3>
            <p class="text-xs" style="color:var(--md-on-surface-v)">Автоматическая отправка сообщений · изменения применяются немедленно</p>
          </div>
          <!-- Enabled toggle -->
          <button @click="toggleSimulation" :disabled="saving.simulation"
            class="flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200"
            :style="simulation.enabled
              ? 'background:rgba(109,213,140,0.15); color:var(--md-success); border:1px solid rgba(109,213,140,0.3)'
              : 'background:var(--md-surface-3); color:var(--md-on-surface-v); border:1px solid var(--md-outline-v)'">
            <span class="w-2 h-2 rounded-full" :class="simulation.enabled ? 'bg-green-400 animate-pulse' : 'bg-slate-500'"></span>
            {{ simulation.enabled ? 'Включена' : 'Выключена' }}
          </button>
        </div>
        <div class="grid grid-cols-2 lg:grid-cols-3 gap-4">
          <div class="lg:col-span-2">
            <label class="label">Интеграция</label>
            <input v-model="simulation.integration" type="text" class="input" placeholder="system1-to-system2" />
          </div>
          <div>
            <label class="label">Интервал (сек)</label>
            <input v-model.number="simulation.intervalSeconds" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Размер пачки</label>
            <input v-model.number="simulation.burstSize" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Дубль каждые N</label>
            <input v-model.number="simulation.duplicateEvery" type="number" min="1" class="input" />
          </div>
          <div>
            <label class="label">Пауза после пачки (сек)</label>
            <input v-model.number="simulation.pauseSeconds" type="number" min="0" class="input" />
          </div>
        </div>
        <div class="flex justify-end">
          <button @click="saveSimulation" :disabled="saving.simulation" class="btn-primary btn-sm">
            {{ saving.simulation ? 'Сохранение...' : 'Применить' }}
          </button>
        </div>
      </div>

    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { coreApi } from '../api/core.js'
import { senderApi } from '../api/sender.js'
import { useToastStore } from '../stores/toast.js'

const toast = useToastStore()
const loading = ref(false)

const saving = reactive({
  scheduler: false,
  resilience: false,
  cleanup: false,
  listener: false,
  simulation: false,
})

const scheduler = reactive({
  outboxFixedDelaySeconds: 5,
  deliveryFixedDelaySeconds: 5,
  replyTimeoutFixedDelaySeconds: 15,
  cleanupFixedDelaySeconds: 86400,
  batchSize: 100,
})

const resilience = reactive({
  outboxRetryDelaySeconds: 10,
  deliveryRetryDelaySeconds: 10,
  replyTimeoutSeconds: 60,
  leaseDurationSeconds: 30,
  maxAttempts: 5,
})

const cleanup = reactive({
  retentionSeconds: 604800,
  batchSize: 500,
})

const listener = reactive({
  inboundConcurrency: 3,
  replyConcurrency: 3,
})

const simulation = reactive({
  enabled: false,
  integration: 'system1-to-system2',
  intervalSeconds: 2,
  duplicateEvery: 2,
  burstSize: 5,
  pauseSeconds: 5,
})

async function loadAll() {
  loading.value = true
  try {
    const [coreRes, simRes] = await Promise.all([
      coreApi.getConfig(),
      senderApi.getSimulationConfig(),
    ])

    const cfg = coreRes.data.data
    Object.assign(scheduler, cfg.scheduler)
    Object.assign(listener, cfg.listener)
    Object.assign(resilience, cfg.resilience)
    Object.assign(cleanup, cfg.cleanup)

    const sim = simRes.data.data
    Object.assign(simulation, sim)
  } catch {
    toast.error('Не удалось загрузить конфигурацию')
  } finally {
    loading.value = false
  }
}

async function saveScheduler() {
  saving.scheduler = true
  try {
    await coreApi.updateScheduler({ ...scheduler })
    toast.success('Планировщик обновлён')
  } catch {
    toast.error('Не удалось сохранить настройки планировщика')
  } finally {
    saving.scheduler = false
  }
}

async function saveResilience() {
  saving.resilience = true
  try {
    await coreApi.updateResilience({ ...resilience })
    toast.success('Параметры устойчивости обновлены')
  } catch {
    toast.error('Не удалось сохранить параметры устойчивости')
  } finally {
    saving.resilience = false
  }
}

async function saveCleanup() {
  saving.cleanup = true
  try {
    await coreApi.updateCleanup({ ...cleanup })
    toast.success('Параметры очистки обновлены')
  } catch {
    toast.error('Не удалось сохранить параметры очистки')
  } finally {
    saving.cleanup = false
  }
}

async function saveListener() {
  saving.listener = true
  try {
    await coreApi.updateListener({ ...listener })
    toast.success('Параметры listener сохранены (вступят в силу после перезапуска)')
  } catch {
    toast.error('Не удалось сохранить параметры listener')
  } finally {
    saving.listener = false
  }
}

async function saveSimulation() {
  saving.simulation = true
  try {
    await senderApi.updateSimulationConfig({ ...simulation })
    toast.success('Параметры симуляции обновлены')
  } catch {
    toast.error('Не удалось сохранить параметры симуляции')
  } finally {
    saving.simulation = false
  }
}

async function toggleSimulation() {
  saving.simulation = true
  try {
    simulation.enabled = !simulation.enabled
    await senderApi.updateSimulationConfig({ enabled: simulation.enabled })
    toast.success(simulation.enabled ? 'Симуляция запущена' : 'Симуляция остановлена')
  } catch {
    simulation.enabled = !simulation.enabled
    toast.error('Не удалось изменить статус симуляции')
  } finally {
    saving.simulation = false
  }
}

function formatDuration(seconds) {
  if (!seconds) return ''
  const d = Math.floor(seconds / 86400)
  const h = Math.floor((seconds % 86400) / 3600)
  const m = Math.floor((seconds % 3600) / 60)
  const s = seconds % 60
  const parts = []
  if (d) parts.push(`${d}д`)
  if (h) parts.push(`${h}ч`)
  if (m) parts.push(`${m}м`)
  if (s) parts.push(`${s}с`)
  return parts.join(' ')
}

onMounted(loadAll)
</script>
