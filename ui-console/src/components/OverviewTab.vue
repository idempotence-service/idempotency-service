<template>
  <div class="space-y-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">Обзор системы</h2>
      </div>
      <button @click="loadAll" :disabled="loading" class="btn-tonal">
        <svg class="w-4 h-4" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        Обновить
      </button>
    </div>

    <!-- Stat Cards -->
    <div class="grid grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4">
      <div v-for="card in statCards" :key="card.label" class="card p-5 flex flex-col gap-3">
        <div class="flex items-center justify-between">
          <span class="text-xs font-semibold uppercase tracking-wider" style="color:var(--md-on-surface-v)">{{ card.label }}</span>
          <div class="w-8 h-8 rounded-xl flex items-center justify-center" :style="{ background: card.bg }">
            <span class="text-base">{{ card.icon }}</span>
          </div>
        </div>
        <div>
          <div class="text-3xl font-bold tracking-tight" :style="{ color: card.color }">
            <span v-if="loading">—</span>
            <span v-else>{{ card.value }}</span>
          </div>
          <p class="text-xs mt-1" style="color:var(--md-on-surface-v)">{{ card.sub }}</p>
        </div>
      </div>
    </div>

    <!-- Activity Timeline Chart -->
    <div class="card p-6">
      <div class="flex items-center justify-between mb-4">
        <h3 class="text-sm font-semibold" style="color:var(--md-on-surface)">Активность за последний час</h3>
        <div class="flex items-center gap-3">
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:#f6c142"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Дубли</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:var(--md-error)"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Таймауты</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:#82b1ff"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Ошибки</span>
          </div>
        </div>
      </div>
      <div v-if="loading" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
        Загрузка...
      </div>
      <div v-else-if="!activityChartData.labels.length" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
        Нет данных за последний час
      </div>
      <div v-else class="h-56">
        <Line :data="activityChartData" :options="activityChartOptions" />
      </div>
    </div>

    <!-- Charts Row: Message Types + Integrations -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">

      <!-- Message Type Distribution -->
      <div class="card p-6">
        <h3 class="text-sm font-semibold mb-4" style="color:var(--md-on-surface)">Сообщения по типам</h3>
        <div v-if="loading" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Загрузка...
        </div>
        <div v-else-if="!(sentCount + receivedCount + totalErrors)" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Нет данных для отображения
        </div>
        <div v-else class="flex flex-col lg:flex-row items-center gap-6 lg:gap-10">
          <div class="relative w-48 h-48 shrink-0">
            <Doughnut :data="typeChartData" :options="typeChartOptions" />
            <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
              <span class="text-3xl font-bold" style="color:var(--md-on-surface)">{{ sentCount + receivedCount + totalErrors }}</span>
              <span class="text-xs" style="color:var(--md-on-surface-v)">всего</span>
            </div>
          </div>
          <div class="flex-1 w-full lg:w-auto space-y-3">
            <div v-for="item in typeBreakdown" :key="item.label" class="flex items-center justify-between p-2 rounded-xl" style="background:var(--md-surface-2)">
              <div class="flex items-center gap-3">
                <div class="w-3 h-3 rounded-full" :style="{ background: item.color }"></div>
                <span class="text-sm font-medium" style="color:var(--md-on-surface)">{{ item.label }}</span>
              </div>
              <div class="flex items-center gap-3">
                <span class="text-sm font-bold" :style="{ color: item.color }">{{ item.count }}</span>
                <span class="text-xs px-2 py-1 rounded-full" style="background:var(--md-surface-3); color:var(--md-on-surface-v)">
                  {{ (sentCount + receivedCount + totalErrors) ? Math.round(item.count / (sentCount + receivedCount + totalErrors) * 100) : 0 }}%
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Integrations -->
      <div class="card p-6">
        <h3 class="text-sm font-semibold mb-4" style="color:var(--md-on-surface)">Интеграции</h3>
        <div v-if="loading" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Загрузка...
        </div>
        <div v-else-if="!integrations.length" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Нет зарегистрированных интеграций
        </div>
        <div v-else class="space-y-2">
          <div
            v-for="intg in integrations" :key="intg.integrationName"
            class="rounded-xl overflow-hidden"
            style="border:1px solid var(--md-outline-v)"
          >
            <button
              class="w-full flex items-center justify-between px-4 py-3 text-left"
              style="background:var(--md-surface-2)"
              @click="toggleIntegration(intg.integrationName)"
            >
              <div class="flex items-center gap-3 min-w-0">
                <span class="text-sm font-semibold mono truncate" style="color:var(--md-on-surface)">{{ intg.integrationName }}</span>
                <span v-if="intg.serviceName" class="text-xs px-2 py-0.5 rounded-full shrink-0" style="background:var(--md-surface-3); color:var(--md-on-surface-v)">{{ intg.serviceName }}</span>
              </div>
              <div class="flex items-center gap-3 shrink-0 ml-3">
                <span
                  class="text-xs px-2.5 py-1 rounded-full font-medium"
                  :style="intg.idempotencyEnabled
                    ? 'background:rgba(109,213,140,0.15); color:var(--md-success)'
                    : 'background:var(--md-surface-3); color:var(--md-on-surface-v)'"
                >
                  {{ intg.idempotencyEnabled ? 'Идемпотентность ✓' : 'Идемпотентность ✗' }}
                </span>
                <svg
                  class="w-4 h-4 transition-transform duration-200"
                  :class="expandedIntegrations.has(intg.integrationName) ? 'rotate-180' : ''"
                  style="color:var(--md-on-surface-v)" fill="none" stroke="currentColor" viewBox="0 0 24 24"
                >
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </div>
            </button>

            <div v-if="expandedIntegrations.has(intg.integrationName)"
                 class="grid grid-cols-2 gap-px p-px"
                 style="background:var(--md-outline-v)">
              <div
                v-for="[label, ch] in channelEntries(intg)" :key="label"
                class="p-3 space-y-1"
                style="background:var(--md-surface-1)"
              >
                <p class="text-xs font-semibold uppercase tracking-wide" style="color:var(--md-primary)">{{ label }}</p>
                <p class="text-xs mono font-medium" style="color:var(--md-on-surface)">{{ ch.topic }}</p>
                <p class="text-xs" style="color:var(--md-on-surface-v)">{{ ch.bootstrapServers }}</p>
                <p v-if="ch.group" class="text-xs" style="color:var(--md-on-surface-v)">group: {{ ch.group }}</p>
                <p class="text-xs" style="color:var(--md-on-surface-v)">{{ ch.partitions }}p · rf {{ ch.replicationFactor }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Bottom row -->
    <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">

      <!-- Recent errors -->
      <div class="card p-6 lg:col-span-2">
        <h3 class="text-sm font-semibold mb-4" style="color:var(--md-on-surface)">Последние события с ошибками</h3>
        <div class="table-container">
          <table class="data-table" v-if="recentEvents.length">
            <thead>
              <tr>
                <th>Global Key</th>
                <th>Статус</th>
                <th>Интеграция</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="ev in recentEvents" :key="ev.globalKey" class="!cursor-default">
                <td class="mono text-xs">{{ truncate(ev.globalKey, 26) }}</td>
                <td><StatusBadge :status="ev.status" /></td>
                <td class="text-xs" style="color:var(--md-on-surface-v)">{{ ev.integration }}</td>
              </tr>
            </tbody>
          </table>
          <p v-else class="text-center py-8 text-sm" style="color:var(--md-on-surface-v)">
            {{ loading ? 'Загрузка...' : 'Ошибок нет' }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Doughnut, Line } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
import { coreApi } from '../api/core.js'
import { senderApi } from '../api/sender.js'
import { receiverApi } from '../api/receiver.js'
import StatusBadge from './StatusBadge.vue'

ChartJS.register(ArcElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend)

const loading = ref(false)
const initialLoading = ref(true)
const lastRefresh = ref('—')
const errorEvents = ref([])
const totalErrors = ref(0)
const sentCount = ref(0)
const repliesCount = ref(0)
const receivedCount = ref(0)
const duplicateCount = ref(0)
const timeoutCount = ref(0)
const auditActivity = ref([])
const integrations = ref([])
const expandedIntegrations = ref(new Set())
const recentEvents = computed(() => errorEvents.value.slice(0, 5))

const statCards = computed(() => [
  {
    label: 'Ошибки',
    value: totalErrors.value + timeoutCount.value,
    sub: `Финальных: ${totalErrors.value} · Таймаутов: ${timeoutCount.value}`,
    icon: '⚠',
    color: 'var(--md-error)',
    bg: 'rgba(242,184,181,0.12)',
  },
  {
    label: 'Дубликаты',
    value: duplicateCount.value,
    sub: 'Заблокировано идемпотенцией',
    icon: '♻',
    color: '#f6c142',
    bg: 'rgba(246,193,66,0.12)',
  },
  {
    label: 'Отправлено',
    value: sentCount.value,
    sub: 'Sender Simulator',
    icon: '↑',
    color: '#82b1ff',
    bg: 'rgba(130,177,255,0.12)',
  },
  {
    label: 'Получено',
    value: receivedCount.value,
    sub: 'Receiver Simulator',
    icon: '↓',
    color: 'var(--md-success)',
    bg: 'rgba(109,213,140,0.12)',
  },
])

const typeBreakdown = computed(() => [
  { label: 'Отправлено',  count: sentCount.value,      color: '#82b1ff' },
  { label: 'Получено',    count: receivedCount.value,  color: '#6dd58c' },
  { label: 'Ошибки',      count: totalErrors.value,    color: '#f2b8b5' },
  { label: 'Дубликаты',   count: duplicateCount.value, color: '#f6c142' },
])

const topErrorIntegrations = computed(() => {
  const counts = {}
  errorEvents.value.forEach(e => {
    counts[e.integration] = (counts[e.integration] || 0) + 1
  })
  return Object.entries(counts)
    .sort((a, b) => b[1] - a[1])
    .slice(0, 5)
    .map(([name, count]) => ({ name, count }))
})

const typeChartData = computed(() => ({
  labels: typeBreakdown.value.map(t => t.label),
  datasets: [{
    data: typeBreakdown.value.map(t => t.count),
    backgroundColor: typeBreakdown.value.map(t => t.color + '33'),
    borderColor:     typeBreakdown.value.map(t => t.color),
    borderWidth: 2,
    hoverOffset: 6,
  }],
}))

// Activity timeline - last 60 minutes (audit events only)
const activityChartData = computed(() => {
  const labels = []
  const duplicateData = []
  const timeoutData = []
  const errorData = []

  const now = new Date()

  // Generate last 12 time slots (5-minute intervals)
  for (let i = 11; i >= 0; i--) {
    const slotTime = new Date(now.getTime() - i * 5 * 60000)
    const label = `${slotTime.getHours().toString().padStart(2, '0')}:${slotTime.getMinutes().toString().padStart(2, '0')}`
    labels.push(label)

    const slotIndex = 11 - i
    const slotActivity = auditActivity.value[slotIndex] || {}

    duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
    timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
    errorData.push(
      (slotActivity['Некорректное входящее событие'] || 0) +
      (slotActivity['Не найден маршрут для входящего события'] || 0) +
      (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
      (slotActivity['Получен ответ без ожидающей операции'] || 0)
    )
  }

  return {
    labels,
    datasets: [
      {
        label: 'Дубли',
        data: duplicateData,
        borderColor: '#f6c142',
        backgroundColor: 'rgba(246,193,66,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Таймауты',
        data: timeoutData,
        borderColor: '#f2b8b5',
        backgroundColor: 'rgba(242,184,181,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Ошибки',
        data: errorData,
        borderColor: '#82b1ff',
        backgroundColor: 'rgba(130,177,255,0.1)',
        tension: 0.4,
        fill: true,
      },
    ],
  }
})

const activityChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { display: false },
    tooltip: {
      mode: 'index',
      intersect: false,
    },
  },
  scales: {
    x: { ticks: { color: '#cac4d0', font: { size: 10 } }, grid: { display: false } },
    y: { ticks: { color: '#cac4d0', font: { size: 10 } }, grid: { color: 'rgba(255,255,255,0.05)' } },
  },
}

const typeChartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  cutout: '65%',
  plugins: {
    legend: { display: false },
    tooltip: {
      callbacks: {
        label: ctx => {
          const total = ctx.dataset.data.reduce((a, b) => a + b, 0)
          const pct = total ? Math.round(ctx.parsed / total * 100) : 0
          return ` ${ctx.label}: ${ctx.parsed} (${pct}%)`
        },
      },
    },
  },
}

const doughnutOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: { position: 'bottom', labels: { color: '#cac4d0', padding: 12, font: { size: 11 } } },
    tooltip: { callbacks: { label: ctx => ` ${ctx.label}: ${ctx.parsed}` } },
  },
}

const barOptions = {
  responsive: true,
  maintainAspectRatio: false,
  indexAxis: 'y',
  plugins: {
    legend: { display: false },
    tooltip: { callbacks: { label: ctx => ` ${ctx.parsed.x} событий` } },
  },
  scales: {
    x: { ticks: { color: '#cac4d0', font: { size: 11 } }, grid: { color: 'rgba(255,255,255,0.05)' } },
    y: { ticks: { color: '#cac4d0', font: { size: 11 } }, grid: { display: false } },
  },
}

function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, 8) + '…' + s.slice(-8) : s
}

async function loadAll() {
  if (initialLoading.value) {
    loading.value = true
  }
  try {
    await Promise.allSettled([
      loadErrors(),
      loadSenderStats(),
      loadReceiverStats(),
      loadDuplicates(),
      loadTimeouts(),
      loadAuditActivity(),
      loadIntegrations(),
    ])
    lastRefresh.value = new Date().toLocaleTimeString('ru-RU')
  } finally {
    loading.value = false
    initialLoading.value = false
  }
}

async function loadErrors() {
  try {
    const res = await coreApi.getErrorEvents({ page: 0, limit: 100, sort: 'desc' })
    const data = res.data?.data
    errorEvents.value = data?.content ?? []
    totalErrors.value = data?.totalElements ?? 0
  } catch {}
}

async function loadSenderStats() {
  try {
    const res = await senderApi.getStats()
    const stats = res.data?.data
    sentCount.value = stats?.totalSent ?? 0
    repliesCount.value = stats?.totalReplies ?? 0
  } catch {}
}

async function loadReceiverStats() {
  try {
    const res = await receiverApi.getStats()
    const stats = res.data?.data
    receivedCount.value = stats?.totalReceived ?? 0
  } catch {}
}

async function loadDuplicates() {
  try {
    const res = await coreApi.getDuplicateCount()
    duplicateCount.value = res.data?.data ?? 0
  } catch {}
}

async function loadTimeouts() {
  try {
    const res = await coreApi.getTimeoutCount()
    timeoutCount.value = res.data?.data ?? 0
  } catch {}
}

async function loadIntegrations() {
  try {
    const res = await coreApi.getIntegrations()
    integrations.value = res.data?.data ?? []
  } catch {}
}

function toggleIntegration(name) {
  const s = new Set(expandedIntegrations.value)
  if (s.has(name)) { s.delete(name) } else { s.add(name) }
  expandedIntegrations.value = s
}

function channelEntries(intg) {
  return [
    ['Inbound', intg.inbound],
    ['Request Out', intg.requestOut],
    ['Reply In', intg.replyIn],
    ['Reply Out', intg.replyOut],
  ].filter(([, ch]) => ch != null)
}

async function loadAuditActivity() {
  const now = new Date()
  const activity = []

  for (let i = 11; i >= 0; i--) {
    const slotTime = new Date(now.getTime() - i * 5 * 60000)
    const since = slotTime.toISOString()
    try {
      const res = await coreApi.getAuditActivity(since)
      activity.push(res.data?.data ?? {})
    } catch {
      activity.push({})
    }
  }

  auditActivity.value = activity
}

onMounted(() => {
  loadAll()
  const interval = setInterval(loadAll, 3000)
  return () => clearInterval(interval)
})
</script>
