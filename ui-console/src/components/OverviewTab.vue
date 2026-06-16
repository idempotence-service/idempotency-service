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

    <!-- System Health Indicator -->
    <div class="card p-4">
      <div class="flex items-center justify-between">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-full flex items-center justify-center"
               :style="systemHealth.status === 'healthy' 
                 ? 'background:rgba(109,213,140,0.15)' 
                 : systemHealth.status === 'degraded' 
                 ? 'background:rgba(246,193,66,0.15)' 
                 : 'background:rgba(242,113,113,0.15)'">
            <div class="w-3 h-3 rounded-full animate-pulse"
                 :style="systemHealth.status === 'healthy' 
                   ? 'background:#6dd58c' 
                   : systemHealth.status === 'degraded' 
                   ? 'background:#f6c142' 
                   : 'background:#f27171'"></div>
          </div>
          <div>
            <h3 class="text-sm font-semibold" style="color:var(--md-on-surface)">
              Статус системы: 
              <span :style="systemHealth.status === 'healthy' 
                ? 'color:#6dd58c' 
                : systemHealth.status === 'degraded' 
                ? 'color:#f6c142' 
                : 'color:#f27171'">
                {{ systemHealth.label }}
              </span>
            </h3>
            <p class="text-xs mt-0.5" style="color:var(--md-on-surface-v)">{{ systemHealth.message }}</p>
          </div>
        </div>
        <div class="text-right">
          <p class="text-xs font-medium" style="color:var(--md-on-surface-v)">Последнее обновление</p>
          <p class="text-sm font-semibold" style="color:var(--md-on-surface)">{{ lastRefresh }}</p>
        </div>
      </div>
    </div>

    <!-- Time Range Selector -->
    <div class="flex items-center justify-center">
      <div class="flex items-center gap-1 p-1 rounded-full" style="background:var(--md-surface-2)">
        <button
          v-for="range in timeRanges"
          :key="range.id"
          @click="activityTimeRange = range.id"
          class="px-3 py-1 rounded-full text-xs font-medium transition-all"
          :style="activityTimeRange === range.id
            ? `background:var(--md-primary); color:var(--md-on-primary)`
            : `color:var(--md-on-surface-v)`"
        >
          {{ range.label }}
        </button>
      </div>
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

    <!-- Activity Timeline Charts -->
    <div class="grid grid-cols-1 lg:grid-cols-2 gap-6">
      <!-- Duplicates Chart -->
      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold" style="color:var(--md-on-surface)">Дубликаты</h3>
          <div class="flex items-center gap-1.5">
            <div class="w-2 h-2 rounded-full" style="background:#f6c142"></div>
            <span class="text-xs" style="color:var(--md-on-surface-v)">Дубли</span>
          </div>
        </div>
        <div v-if="loading" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
          Загрузка...
        </div>
        <div v-else-if="!activityChartData.labels.length" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
          Нет данных
        </div>
        <div v-else class="h-56">
          <Line :data="duplicatesChartData" :options="activityChartOptions" />
        </div>
      </div>

      <!-- Errors & Timeouts Chart -->
      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold" style="color:var(--md-on-surface)">Проблемы обработки</h3>
          <div class="flex items-center gap-3">
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
          Нет данных
        </div>
        <div v-else class="h-56">
          <Line :data="errorsChartData" :options="activityChartOptions" />
        </div>
      </div>

      <!-- Sent/Received Chart -->
      <div class="card p-6">
        <div class="flex items-center justify-between mb-4">
          <h3 class="text-sm font-semibold" style="color:var(--md-on-surface)">Отправлено / Получено</h3>
          <div class="flex items-center gap-3">
            <div class="flex items-center gap-1.5">
              <div class="w-2 h-2 rounded-full" style="background:#82b1ff"></div>
              <span class="text-xs" style="color:var(--md-on-surface-v)">Отправлено</span>
            </div>
            <div class="flex items-center gap-1.5">
              <div class="w-2 h-2 rounded-full" style="background:#6dd58c"></div>
              <span class="text-xs" style="color:var(--md-on-surface-v)">Получено</span>
            </div>
          </div>
        </div>
        <div v-if="loading" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
          Загрузка...
        </div>
        <div v-else-if="!messageChartData.labels.length" class="flex items-center justify-center h-56 text-sm" style="color:var(--md-on-surface-v)">
          Нет данных
        </div>
        <div v-else class="h-56">
          <Line :data="messageChartData" :options="activityChartOptions" />
        </div>
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
        <div v-else-if="!(sentCount + receivedCount + totalErrorsFromAudit)" class="flex items-center justify-center h-44 text-sm" style="color:var(--md-on-surface-v)">
          Нет данных для отображения
        </div>
        <div v-else class="flex flex-col lg:flex-row items-center gap-6 lg:gap-10">
          <div class="relative w-48 h-48 shrink-0">
            <Doughnut :data="typeChartData" :options="typeChartOptions" />
            <div class="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
              <span class="text-3xl font-bold" style="color:var(--md-on-surface)">{{ sentCount + receivedCount + totalErrorsFromAudit }}</span>
              <span class="text-xs" style="color:var(--md-on-surface-v)">всего</span>
            </div>
          </div>
          <div class="flex-1 w-full lg:w-auto space-y-3">
            <div v-for="item in typeBreakdown" :key="item.label" class="flex items-center gap-4 p-2 rounded-xl" style="background:var(--md-surface-2)">
              <div class="w-3 h-3 rounded-full shrink-0" :style="{ background: item.color }"></div>
              <span class="text-sm font-medium" style="color:var(--md-on-surface)">{{ item.label }}</span>
              <div class="flex items-center gap-2 ml-auto">
                <span class="text-sm font-bold" :style="{ color: item.color }">{{ item.count }}</span>
                <span class="text-xs px-2 py-1 rounded-full" style="background:var(--md-surface-3); color:var(--md-on-surface-v)">
                  {{ (sentCount + receivedCount + totalErrorsFromAudit) ? Math.round(item.count / (sentCount + receivedCount + totalErrorsFromAudit) * 100) : 0 }}%
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

      <!-- Recent errors from idempotency table -->
      <div class="card p-6 lg:col-span-3">
        <h3 class="text-sm font-semibold mb-4" style="color:var(--md-on-surface)">Операции с ошибкой (таблица idempotency)</h3>
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
            {{ loading ? 'Загрузка...' : 'Операций со статусом ERROR нет' }}
          </p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { Doughnut, Line } from 'vue-chartjs'
import { Chart as ChartJS, ArcElement, LineElement, PointElement, CategoryScale, LinearScale, Tooltip, Legend } from 'chart.js'
import { coreApi } from '../api/core.js'
import { senderApi } from '../api/sender.js'
import { receiverApi } from '../api/receiver.js'
import StatusBadge from './StatusBadge.vue'
import { truncate, getAdaptiveTimeRange, getAdaptiveTimeRangeForData, calculateSuccessRate, formatSuccessRate, calculateThroughput, getThroughputLabel, calculateSystemHealth, buildActivityChartData, buildDuplicatesChartData, buildErrorsChartData, calculateSinceTimestamp, toggleIntegration, channelEntries } from '../utils/overviewHelpers.js'

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
const activityTimeRange = ref('hour') // 'minute', 'hour', 'day'
const integrations = ref([])
const expandedIntegrations = ref(new Set())
const recentEvents = computed(() => errorEvents.value.slice(0, 5))
const sentMessages = ref([])
const receivedMessages = ref([])

const timeRanges = [
  { id: 'minute', label: '1 мин' },
  { id: 'hour', label: '1 час' },
  { id: 'day', label: '1 день' },
  { id: 'all', label: 'Все время' },
]

const statCards = computed(() => [
  {
    label: 'Success Rate',
    value: successRate.value,
    sub: `${successfulMessages.value} из ${totalMessages.value} успешно`,
    icon: '✓',
    color: successRateNumeric.value >= 95 ? '#6dd58c' : successRateNumeric.value >= 80 ? '#f6c142' : '#f27171',
    bg: successRateNumeric.value >= 95 ? 'rgba(109,213,140,0.12)' : successRateNumeric.value >= 80 ? 'rgba(246,193,66,0.12)' : 'rgba(242,113,113,0.12)',
  },
  {
    label: 'Throughput',
    value: throughput.value,
    sub: throughputLabel.value,
    icon: '⚡',
    color: '#82b1ff',
    bg: 'rgba(130,177,255,0.12)',
  },
  {
    label: 'Проблемы обработки',
    value: totalErrors.value + timeoutCount.value,
    sub: `Обработок: ${totalErrors.value} · Таймаутов: ${timeoutCount.value}`,
    icon: '⚠',
    color: 'var(--md-error)',
    bg: 'rgba(242,184,181,0.12)',
    tooltip: 'Включает: некорректные события, ошибки маршрутизации, неверные ответы, несогласованные ответы и таймауты (из event_audit)',
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
    value: sentCountInRange.value,
    sub: '',
    icon: '↑',
    color: '#82b1ff',
    bg: 'rgba(130,177,255,0.12)',
  },
  {
    label: 'Получено',
    value: receivedCountInRange.value,
    sub: '',
    icon: '↓',
    color: 'var(--md-success)',
    bg: 'rgba(109,213,140,0.12)',
  },
])

const typeBreakdown = computed(() => [
  { label: 'Отправлено',  count: sentCountInRange.value,      color: '#82b1ff' },
  { label: 'Получено',    count: receivedCountInRange.value,  color: '#6dd58c' },
  { label: 'Проблемы обработки', count: totalErrors.value,    color: '#f2b8b5' },
  { label: 'Таймауты',   count: timeoutCount.value, color: '#ff6b6b' },
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

// Activity timeline - based on selected time range
const activityChartData = computed(() => {
  const labels = []
  const duplicateData = []
  const timeoutData = []
  const errorData = []

  const now = new Date()

  let slots, intervalMs, formatLabel
  if (activityTimeRange.value === 'all') {
    const totalSlots = auditActivity.value.length
    if (totalSlots === 0) {
      return { labels: [], datasets: [] }
    }
    
    const now = new Date()
    const oldestSlotTime = new Date(now.getTime() - (totalSlots - 1) * intervalMs)
    const timeRange = getAdaptiveTimeRangeForData(oldestSlotTime.getTime(), now.getTime(), totalSlots)
    
    slots = Math.min(totalSlots, 48)
    intervalMs = timeRange.interval
    formatLabel = timeRange.format
    
    for (let i = slots - 1; i >= 0; i--) {
      const slotTime = new Date(now.getTime() - i * intervalMs)
      labels.push(formatLabel(slotTime))
      const slotIndex = slots - 1 - i
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
  } else if (activityTimeRange.value === 'minute') {
    slots = 6
    intervalMs = 10 * 1000 // 10 seconds
    // Align to start of current minute
    const minuteStart = new Date(now)
    minuteStart.setSeconds(0, 0)
    formatLabel = (d, i) => `${i * 10}с`
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(minuteStart.getTime() + i * intervalMs)
      labels.push(formatLabel(slotTime, i))

      const slotActivity = auditActivity.value[i] || {}

      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
  } else if (activityTimeRange.value === 'hour') {
    slots = 12
    intervalMs = 5 * 60000 // 5 minutes
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
    
    for (let i = slots - 1; i >= 0; i--) {
      const slotTime = new Date(now.getTime() - i * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotIndex = slots - 1 - i
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
  } else { // day
    slots = 24
    intervalMs = 60 * 60000 // 1 hour
    formatLabel = (d) => `${d.getHours().toString().padStart(2, '0')}:00`
    
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    
    for (let i = 0; i < slots; i++) {
      const slotTime = new Date(startOfDay.getTime() + i * intervalMs)
      labels.push(formatLabel(slotTime))

      const slotActivity = auditActivity.value[i] || {}

      duplicateData.push(slotActivity['Событие не прошло проверку на идемпотентность'] || 0)
      timeoutData.push(slotActivity['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
      errorData.push(
        (slotActivity['Некорректное входящее событие'] || 0) +
        (slotActivity['Не найден маршрут для входящего события'] || 0) +
        (slotActivity['Некорректный ответ от системы-получателя'] || 0) +
        (slotActivity['Получен ответ без ожидающей операции'] || 0)
      )
    }
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

// Sent/Received message timeline
const messageChartData = computed(() => {
  return buildActivityChartData(sentMessages.value, receivedMessages.value, auditActivity.value, activityTimeRange.value, getAdaptiveTimeRange)
})

// Duplicates chart data (separate)
const duplicatesChartData = computed(() => {
  return buildDuplicatesChartData(auditActivity.value, activityTimeRange.value, getAdaptiveTimeRange)
})

// Errors & timeouts chart data (separate)
const errorsChartData = computed(() => {
  return buildErrorsChartData(auditActivity.value, activityTimeRange.value, getAdaptiveTimeRange, getAdaptiveTimeRangeForData)
})

// Calculate totals from auditActivity for consistency with chart
const totalErrorsFromAudit = computed(() => {
  return auditActivity.value.reduce((sum, slot) => {
    return sum +
      (slot['Некорректное входящее событие'] || 0) +
      (slot['Не найден маршрут для входящего события'] || 0) +
      (slot['Некорректный ответ от системы-получателя'] || 0) +
      (slot['Получен ответ без ожидающей операции'] || 0)
  }, 0)
})

const timeoutCountFromAudit = computed(() => {
  return auditActivity.value.reduce((sum, slot) => {
    return sum + (slot['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
  }, 0)
})

const duplicateCountFromAudit = computed(() => {
  return auditActivity.value.reduce((sum, slot) => {
    return sum + (slot['Событие не прошло проверку на идемпотентность'] || 0)
  }, 0)
})

// Filter sent/received messages by selected time range for stat cards
const sentCountInRange = computed(() => {
  return sentCount.value
})

const receivedCountInRange = computed(() => {
  return receivedCount.value
})

// Success Rate calculation
const totalMessages = computed(() => sentCountInRange.value)
const successfulMessages = computed(() => {
  return sentCountInRange.value - (totalErrors.value + timeoutCount.value)
})
const successRateNumeric = computed(() => {
  return calculateSuccessRate(sentCountInRange.value, totalErrors.value, timeoutCount.value)
})
const successRate = computed(() => {
  return formatSuccessRate(sentCountInRange.value, totalErrors.value, timeoutCount.value)
})

// Throughput calculation (messages per second)
const throughput = computed(() => {
  return calculateThroughput(sentCountInRange.value, activityTimeRange.value)
})

const throughputLabel = computed(() => {
  return getThroughputLabel(activityTimeRange.value)
})

// System Health indicator
const systemHealth = computed(() => {
  return calculateSystemHealth(sentCountInRange.value, totalErrors.value, timeoutCount.value)
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


async function loadAll() {
  if (initialLoading.value) {
    loading.value = true
  }
  try {
    const since = calculateSinceTimestamp(activityTimeRange.value)
    
    await Promise.allSettled([
      loadErrors(),
      loadSenderStats(since),
      loadReceiverStats(since),
      loadAuditActivity(),
      loadIntegrations(),
      loadSentMessages(),
      loadReceivedMessages(),
      loadDuplicates(),
      loadTimeouts(),
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

async function loadSenderStats(since) {
  try {
    const res = await senderApi.getStats(since)
    const stats = res.data?.data
    sentCount.value = stats?.totalSent ?? 0
    repliesCount.value = stats?.totalReplies ?? 0
  } catch {}
}

async function loadReceiverStats(since) {
  try {
    const res = await receiverApi.getStats(since)
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
  } catch (e) {
    console.error('Failed to load integrations:', e)
  }
}

async function loadSentMessages(since) {
  try {
    const res = await senderApi.getSentMessages(since)
    const data = res.data?.data
    sentMessages.value = Array.isArray(data) ? data : (data ? [data] : [])
  } catch {}
}

async function loadReceivedMessages(since) {
  try {
    const res = await receiverApi.getReceivedEvents(since)
    const data = res.data?.data
    receivedMessages.value = Array.isArray(data) ? data : (data ? [data] : [])
  } catch {}
}

function toggleIntegrationLocal(name) {
  expandedIntegrations.value = toggleIntegration(expandedIntegrations.value, name)
}

async function loadAuditActivity() {
  const now = new Date()
  const activity = []

  let slots, intervalMs, startTime
  if (activityTimeRange.value === 'all') {
    // For 'all', load data from epoch start with multiple slots for better visualization
    startTime = new Date(0) // Unix epoch
    slots = 48 // 48 slots for better chart resolution
    intervalMs = (now.getTime() - startTime.getTime()) / slots
  } else if (activityTimeRange.value === 'minute') {
    slots = 6
    intervalMs = 10 * 1000 // 10 seconds
    startTime = new Date(now.getTime() - slots * intervalMs)
  } else if (activityTimeRange.value === 'hour') {
    slots = 12
    intervalMs = 5 * 60000 // 5 minutes
    startTime = new Date(now.getTime() - slots * intervalMs)
  } else { // day
    slots = 24
    intervalMs = 60 * 60000 // 1 hour
    startTime = new Date(now)
    startTime.setHours(0, 0, 0, 0)
  }

  for (let i = 0; i < slots; i++) {
    const slotTime = new Date(startTime.getTime() + i * intervalMs)
    const slotSince = slotTime.toISOString()
    try {
      const res = await coreApi.getAuditActivity(slotSince)
      activity.push(res.data?.data ?? {})
    } catch {
      activity.push({})
    }
  }

  auditActivity.value = activity
}

onMounted(() => {
  loadAll()
  const interval = setInterval(loadAll, 10000)
  onUnmounted(() => clearInterval(interval))
})

watch(activityTimeRange, () => {
  loadAuditActivity()
  
  const now = new Date()
  let since
  if (activityTimeRange.value === 'minute') {
    since = new Date(now.getTime() - 60 * 1000).toISOString()
  } else if (activityTimeRange.value === 'hour') {
    since = new Date(now.getTime() - 60 * 60 * 1000).toISOString()
  } else if (activityTimeRange.value === 'day') {
    const startOfDay = new Date(now)
    startOfDay.setHours(0, 0, 0, 0)
    since = startOfDay.toISOString()
  } else { // all - no time filter
    since = undefined
  }
  
  loadSenderStats(since)
  loadReceiverStats(since)
  loadSentMessages()
  loadReceivedMessages()
})
</script>
