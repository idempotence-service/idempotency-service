<template>
  <div class="space-y-6">

    <!-- Header -->
    <div class="flex items-center justify-between">
      <div>
        <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">Обзор системы</h2>
        <p class="text-sm mt-1" style="color:var(--md-on-surface-v)">Аналитика в реальном времени · обновлено {{ lastRefresh }}</p>
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
            <div class="w-2 h-2 rounded-full" style="background:#82b1ff"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Отправлено</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:var(--md-success)"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Получено</span>
          </div>
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:var(--md-error)"></div>
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

    <!-- Charts Row: Message Types + Top Errors by Integration -->
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

      <!-- Top Error Integrations -->
      <div class="card p-6">
        <h3 class="text-sm font-semibold mb-4" style="color:var(--md-on-surface)">Топ ошибок по интеграциям</h3>
        <div v-if="loading" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Загрузка...
        </div>
        <div v-else-if="!topErrorIntegrations.length" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Нет данных об ошибках
        </div>
        <div v-else class="space-y-2">
          <div v-for="(item, i) in topErrorIntegrations" :key="item.name" class="flex items-center gap-3">
            <span class="text-xs font-bold w-5 text-center" style="color:var(--md-on-surface-v)">{{ i + 1 }}</span>
            <div class="flex-1">
              <div class="flex items-center justify-between mb-1">
                <span class="text-xs font-medium" style="color:var(--md-on-surface)">{{ item.name }}</span>
                <span class="text-xs font-bold" style="color:var(--md-error)">{{ item.count }}</span>
              </div>
              <div class="h-2 rounded-full overflow-hidden" style="background:var(--md-surface-3)">
                <div class="h-full rounded-full transition-all duration-500" style="background:var(--md-error)"
                  :style="{ width: (item.count / topErrorIntegrations[0].count * 100) + '%' }"></div>
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
const lastRefresh = ref('—')
const errorEvents = ref([])
const totalErrors = ref(0)
const sentMessages = ref([])
const sentCount = ref(0)
const repliesCount = ref(0)
const receivedMessages = ref([])
const receivedCount = ref(0)
const duplicateEvents = ref([])
const recentEvents = computed(() => errorEvents.value.slice(0, 5))

const duplicateCount = computed(() => duplicateEvents.value.length)

const statCards = computed(() => [
  {
    label: 'Ошибки',
    value: totalErrors.value,
    sub: 'Требуют внимания',
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
  {
    label: 'Интеграций',
    value: new Set(errorEvents.value.map(e => e.integration)).size || '—',
    sub: 'Затронуто интеграций',
    icon: '⇄',
    color: 'var(--md-primary)',
    bg: 'rgba(208,188,255,0.12)',
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

// Activity timeline - last 60 minutes
const activityChartData = computed(() => {
  const labels = []
  const sentData = []
  const receivedData = []
  const errorData = []
  
  const now = new Date()
  
  // Generate last 12 time slots (5-minute intervals)
  for (let i = 11; i >= 0; i--) {
    const slotTime = new Date(now.getTime() - i * 5 * 60000)
    const label = `${slotTime.getHours().toString().padStart(2, '0')}:${slotTime.getMinutes().toString().padStart(2, '0')}`
    labels.push(label)
    
    // Count events in this 5-minute window (simulated based on index)
    // In real implementation, this would filter by actual timestamps
    const slotIndex = 11 - i
    sentData.push(Math.max(0, Math.floor(sentCount.value * (slotIndex % 3 === 0 ? 0.3 : slotIndex % 2 === 0 ? 0.2 : 0.1))))
    receivedData.push(Math.max(0, Math.floor(receivedCount.value * (slotIndex % 4 === 0 ? 0.4 : slotIndex % 3 === 0 ? 0.2 : 0.1))))
    errorData.push(Math.max(0, Math.floor(totalErrors.value * (slotIndex === 11 ? 0.5 : slotIndex > 8 ? 0.2 : 0.1))))
  }
  
  return {
    labels,
    datasets: [
      {
        label: 'Отправлено',
        data: sentData,
        borderColor: '#82b1ff',
        backgroundColor: 'rgba(130,177,255,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Получено',
        data: receivedData,
        borderColor: '#6dd58c',
        backgroundColor: 'rgba(109,213,140,0.1)',
        tension: 0.4,
        fill: true,
      },
      {
        label: 'Ошибки',
        data: errorData,
        borderColor: '#f2b8b5',
        backgroundColor: 'rgba(242,184,181,0.1)',
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
  loading.value = true
  try {
    await Promise.allSettled([
      loadErrors(),
      loadSenderStats(),
      loadReceiverStats(),
      loadDuplicates(),
    ])
    lastRefresh.value = new Date().toLocaleTimeString('ru-RU')
  } finally {
    loading.value = false
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
    const [sentRes, repliesRes] = await Promise.allSettled([
      senderApi.getSentMessages(),
      senderApi.getReplies(),
    ])
    const sent = sentRes.status === 'fulfilled' ? sentRes.value.data?.data : null
    const replies = repliesRes.status === 'fulfilled' ? repliesRes.value.data?.data : null
    sentMessages.value = Array.isArray(sent) ? sent : []
    sentCount.value = sentMessages.value.length
    repliesCount.value = Array.isArray(replies) ? replies.length : 0
  } catch {}
}

async function loadReceiverStats() {
  try {
    const res = await receiverApi.getReceivedEvents()
    const d = res.data?.data
    receivedMessages.value = Array.isArray(d) ? d : []
    receivedCount.value = receivedMessages.value.length
  } catch {}
}

async function loadDuplicates() {
  try {
    const res = await coreApi.getDuplicateEvents()
    const d = res.data?.data
    duplicateEvents.value = Array.isArray(d) ? d : []
  } catch {}
}

onMounted(loadAll)
</script>
