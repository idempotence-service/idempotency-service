<template>
  <div class="space-y-5">

    <!-- Header -->
    <div class="flex items-center justify-between gap-4">
      <div>
        <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">Сообщения</h2>
      </div>
      <div class="flex items-center gap-3 shrink-0">
        <button
          @click="autoRefresh = !autoRefresh"
          class="btn-tonal btn-sm"
          :style="autoRefresh ? 'color:var(--md-success)' : ''"
        >
          {{ autoRefresh ? '⏸ Пауза' : '▶ Авто' }}
        </button>
        <button @click="loadAll" :disabled="loading" class="btn-tonal">
          <svg class="w-4 h-4" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
          </svg>
          Обновить
        </button>
      </div>
    </div>

    <!-- Compact filter bar -->
    <div class="flex flex-col gap-4">
      <!-- Type chips -->
      <div class="flex items-center gap-2 p-2 rounded-full"
        style="background:var(--md-surface-2); border:1px solid var(--md-outline-v)">
        <button
          v-for="f in filters" :key="f.id"
          @click="activeFilter = f.id"
          class="flex items-center gap-2.5 px-4 py-2.5 rounded-full text-sm font-medium transition-all duration-150"
          :style="activeFilter === f.id
            ? `background:${f.activeBg}; color:${f.activeColor}`
            : `color:var(--md-on-surface-v)`"
        >
          <span class="text-base">{{ f.icon }}</span>
          <span>{{ f.label }}</span>
          <span class="text-xs px-2 py-1 rounded-full font-bold ml-0.5"
            :style="activeFilter === f.id ? 'background:rgba(0,0,0,0.25)' : 'background:var(--md-surface-3)'">
            {{ f.count }}
          </span>
        </button>
      </div>

      <!-- Search -->
      <div class="relative min-w-52">
        <svg class="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none" style="color:var(--md-outline)"
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"/>
        </svg>
        <input v-model="search" type="text" class="input pl-10 pr-10" placeholder="Поиск по key / интеграции / статусу..." />
        <button
          v-if="search"
          @click="search = ''"
          class="absolute right-3.5 top-1/2 -translate-y-1/2 flex items-center justify-center w-5 h-5 rounded-full hover:bg-gray-200 dark:hover:bg-gray-700 transition-colors"
          style="color:var(--md-outline)"
        >
          <svg class="w-3 h-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
          </svg>
        </button>
      </div>
    </div>

    <!-- Table -->
    <div class="card overflow-hidden" style="position:relative; min-height:300px">
      <div
        ref="scrollContainer"
        @scroll="handleScroll"
        style="max-height:600px; overflow-y:auto"
      >

      <!-- Skeleton overlay (shown only during initial load, doesn't remove table from DOM) -->
      <div v-if="isInitialLoading" class="absolute inset-0 z-10" style="background:var(--md-surface); min-height:300px">
        <div class="flex items-center gap-2 px-4 py-3" style="border-bottom:1px solid var(--md-outline-v)">
          <div class="h-4 w-16 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          <div class="h-4 w-24 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          <div class="h-4 w-20 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          <div class="h-4 w-28 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
        </div>
        <div v-for="n in 6" :key="n" class="flex items-center gap-2 px-4 py-4" style="border-bottom:1px solid var(--md-outline-v); height:56px">
          <div class="flex items-center gap-3 w-[160px]">
            <div class="w-9 h-9 rounded-xl animate-pulse" style="background:var(--md-surface-3)"></div>
            <div class="h-3 w-16 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          </div>
          <div class="w-[240px] h-3 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          <div class="w-[140px] h-3 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
          <div class="w-[180px] h-3 rounded animate-pulse" style="background:var(--md-surface-3)"></div>
        </div>
      </div>

      <!-- Empty state -->
      <div v-if="!isInitialLoading && !displayedMessages.length && hasLoadedOnce"
        class="flex flex-col items-center justify-center py-20 gap-3"
        style="color:var(--md-on-surface-v); min-height:300px">
        <svg class="w-12 h-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
        </svg>
        <p class="text-sm">Нет сообщений в этой категории</p>
      </div>

      <!-- Data table - always rendered with v-show to prevent DOM removal -->
      <table v-show="displayedMessages.length" class="data-table" style="table-layout:fixed; min-height:300px">
        <colgroup>
          <col style="width:160px">
          <col style="width:240px">
          <col style="width:140px">
          <col style="width:220px">
        </colgroup>
        <thead>
          <tr style="height:48px">
            <th @click="toggleSort('type')" class="cursor-pointer select-none py-3">
              <div class="flex items-center gap-2 h-full">
                Тип
                <svg v-if="sortBy==='type'" class="w-3.5 h-3.5" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="sortDir==='asc'?'M5 15l7-7 7 7':'M19 9l-7 7-7-7'"/>
                </svg>
                <svg v-else class="w-3.5 h-3.5" style="color:var(--md-outline);opacity:0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l4-4 4 4m0 6l-4 4-4-4"/>
                </svg>
              </div>
            </th>
            <th @click="toggleSort('key')" class="cursor-pointer select-none py-3">
              <div class="flex items-center gap-2 h-full">
                Key / UID
                <svg v-if="sortBy==='key'" class="w-3.5 h-3.5" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="sortDir==='asc'?'M5 15l7-7 7 7':'M19 9l-7 7-7-7'"/>
                </svg>
                <svg v-else class="w-3.5 h-3.5" style="color:var(--md-outline);opacity:0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l4-4 4 4m0 6l-4 4-4-4"/>
                </svg>
              </div>
            </th>
            <th @click="toggleSort('integration')" class="cursor-pointer select-none py-3">
              <div class="flex items-center gap-2 h-full">
                Интеграция
                <svg v-if="sortBy==='integration'" class="w-3.5 h-3.5" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="sortDir==='asc'?'M5 15l7-7 7 7':'M19 9l-7 7-7-7'"/>
                </svg>
                <svg v-else class="w-3.5 h-3.5" style="color:var(--md-outline);opacity:0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 9l4-4 4 4m0 6l-4 4-4-4"/>
                </svg>
              </div>
            </th>
            <th class="py-3">Описание</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="(msg, i) in displayedMessages"
            :key="msg._id || i"
            class="cursor-pointer group"
            style="height:56px"
            @click="selectedMsg = msg"
          >
            <!-- Type -->
            <td class="py-3">
              <div class="flex items-center gap-3 h-full">
                <div class="w-9 h-9 rounded-xl flex items-center justify-center text-base shrink-0"
                  :style="{ background: typeStyle(msg.type).bg }">
                  {{ typeStyle(msg.type).icon }}
                </div>
                <span class="text-xs font-semibold whitespace-nowrap" :style="{ color: typeStyle(msg.type).color }">
                  {{ typeStyle(msg.type).label }}
                </span>
              </div>
            </td>
            <!-- Key -->
            <td class="py-3">
              <div class="flex items-center gap-2 h-full">
                <span class="mono text-xs" style="color:var(--md-on-surface)">{{ truncate(msg.key, 22) }}</span>
                <button v-if="msg.key" @click.stop="copy(msg.key)"
                  class="flex items-center justify-center w-8 h-8 rounded-lg opacity-0 group-hover:opacity-100 shrink-0 transition-all"
                  style="background:var(--md-surface-3); color:var(--md-on-surface-v)"
                  title="Копировать">
                  <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z"/>
                  </svg>
                </button>
              </div>
            </td>
            <!-- Integration -->
            <td class="py-3">
              <div class="flex items-center h-full">
                <span class="text-xs mono px-2 py-1 rounded-lg" style="background:var(--md-surface-3); color:var(--md-on-surface-v)">{{ msg.integration || '—' }}</span>
              </div>
            </td>
            <!-- Description -->
            <td class="py-3">
              <div class="flex items-center h-full">
                <span class="text-xs" style="color:var(--md-on-surface-v)">
                  {{ msg.description ? truncate(msg.description, 38) : '—' }}
                </span>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Detail Modal -->
      <Teleport to="body">
        <Transition name="modal">
          <div v-if="selectedMsg"
            class="fixed inset-0 z-50 flex items-center justify-center p-4"
            @click.self="selectedMsg = null">
            <div class="absolute inset-0 bg-black/55 backdrop-blur-sm" @click="selectedMsg = null"/>
            <div class="relative w-full max-w-lg rounded-2xl shadow-2xl overflow-hidden"
              style="background:var(--md-surface-2); border:1px solid var(--md-outline-v)">

              <!-- Modal header -->
              <div class="flex items-center justify-between px-6 py-4"
                style="border-bottom:1px solid var(--md-outline-v)">
                <div class="flex items-center gap-3">
                  <div class="w-9 h-9 rounded-xl flex items-center justify-center text-lg"
                    :style="{ background: typeStyle(selectedMsg.type).bg }">
                    {{ typeStyle(selectedMsg.type).icon }}
                  </div>
                  <div>
                    <p class="text-sm font-semibold" style="color:var(--md-on-surface)">
                      {{ typeStyle(selectedMsg.type).label }}
                    </p>
                    <p class="text-xs mono" style="color:var(--md-on-surface-v)">
                      {{ truncate(selectedMsg.key, 30) }}
                    </p>
                  </div>
                </div>
                <button @click="selectedMsg = null" class="btn-icon">
                  <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"/>
                  </svg>
                </button>
              </div>

              <!-- Modal body: raw fields -->
              <div class="px-6 py-5 max-h-[60vh] overflow-y-auto space-y-2">
                <div
                  v-for="[k, v] in rawFields"
                  :key="k"
                  class="flex gap-3 py-2"
                  style="border-bottom:1px solid var(--md-outline-v)">
                  <span class="text-xs font-semibold uppercase tracking-wider w-36 shrink-0 pt-0.5"
                    style="color:var(--md-on-surface-v)">{{ k }}</span>
                  <span class="text-xs mono break-all" style="color:var(--md-on-surface)">{{ v }}</span>
                </div>
              </div>

              <!-- Modal footer -->
              <div class="flex items-center justify-end gap-3 px-6 py-4"
                style="border-top:1px solid var(--md-outline-v)">
                <button
                  v-if="selectedMsg.type === 'error'"
                  @click="retryEvent(selectedMsg.key); selectedMsg = null"
                  :disabled="retryingKey === selectedMsg.key"
                  class="btn-danger">
                  ↻ Перезапустить
                </button>
                <button @click="selectedMsg = null" class="btn-tonal">Закрыть</button>
              </div>
            </div>
          </div>
        </Transition>
      </Teleport>

      <!-- Footer info -->
      <div class="flex items-center justify-between px-4 py-3 text-xs"
        style="border-top:1px solid var(--md-outline-v); color:var(--md-on-surface-v)">
        <span class="flex items-center gap-2">
          <!-- Refresh indicator - always in DOM to prevent layout shift -->
          <span v-show="isRefreshing" class="w-3 h-3 rounded-full animate-pulse shrink-0" style="background:var(--md-primary)"></span>
          <span v-show="!isRefreshing" class="w-3 h-3 shrink-0"></span>
          <span>
            Показано <strong class="tabular-nums" style="color:var(--md-on-surface); font-variant-numeric: tabular-nums">{{ displayedMessages.length }}</strong>
            из <strong class="tabular-nums" style="color:var(--md-on-surface); font-variant-numeric: tabular-nums">{{ allMessages.length }}</strong> записей
          </span>
        </span>
        <span v-show="loadingMore" class="italic">
          Загрузка...
        </span>
        <span v-show="displayedMessages.length >= allMessages.length && allMessages.length > displayLimit.value" class="italic">
          Загружены все записи
        </span>
      </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { coreApi } from '../api/core.js'
import { senderApi } from '../api/sender.js'
import { receiverApi } from '../api/receiver.js'
import { useToastStore } from '../stores/toast.js'
import StatusBadge from './StatusBadge.vue'

const toast = useToastStore()

const loading = ref(false)
const lastRefresh = ref('—')
const autoRefresh = ref(true)
const search = ref('')
const activeFilter = ref('all')
const maxDisplay = 200
const displayLimit = ref(200)
const loadingMore = ref(false)
const retryingKey = ref(null)
const hasLoadedOnce = ref(false)

const sentMessages   = ref([])
const receivedEvents = ref([])
const errorEvents    = ref([])
const duplicateEvents = ref([])
const auditActivity = ref([])
const senderStats = ref({ totalSent: 0, totalReplies: 0 })
const receiverStats = ref({ totalReceived: 0 })
const totalErrorCount = ref(0)
const totalDuplicateCount = ref(0)

const sortBy  = ref('')
const sortDir = ref('asc')
const selectedMsg = ref(null)
const allMessages = ref([])
const scrollContainer = ref(null)

// Loading states - no flash on refresh
const isInitialLoading = computed(() => loading.value && !allMessages.value.length)
const isRefreshing = computed(() => loading.value && allMessages.value.length > 0)

function toggleSort(col) {
  if (sortBy.value === col) sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  else { sortBy.value = col; sortDir.value = 'asc' }
}

function handleScroll() {
  if (!scrollContainer.value || loadingMore.value) return

  const container = scrollContainer.value
  const scrollBottom = container.scrollHeight - container.scrollTop - container.clientHeight

  // Load more when user is within 100px of bottom
  if (scrollBottom < 100 && displayLimit.value < allMessages.value.length) {
    loadingMore.value = true
    // Simulate async loading with small delay for smooth UX
    setTimeout(() => {
      displayLimit.value = Math.min(displayLimit.value + 100, allMessages.value.length)
      loadingMore.value = false
    }, 100)
  }
}

async function loadAuditActivity() {
  try {
    const now = new Date()
    const oneHourAgo = new Date(now.getTime() - 60 * 60000).toISOString()
    const res = await coreApi.getAuditActivity(oneHourAgo)
    auditActivity.value = res.data?.data || []
  } catch {
    auditActivity.value = []
  }
}

function rebuildMessages() {
  const list = []

  // Keys that were sent (to detect duplicates)
  const sentKeySet = new Set(
    sentMessages.value.map(m => m.uid || m.id || m.globalKey).filter(Boolean)
  )

  sentMessages.value.forEach(m => {
    const baseKey = m.uid || m.id || JSON.stringify(m)
    list.push({
      _id:         `sent-${baseKey}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:        'sent',
      key:          m.uid || m.id || '',
      integration:  m.integration || '',
      status:       m.status || 'SENT',
      description:  m.description || '',
    })
  })

  receivedEvents.value.forEach(m => {
    const baseKey = m.globalKey || m.uid || JSON.stringify(m)
    list.push({
      _id:         `recv-${baseKey}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:        'received',
      key:          m.globalKey || m.uid || '',
      integration:  m.integration || '',
      status:       m.status || m.result || 'RECEIVED',
      description:  m.resultDescription || '',
    })
  })

  errorEvents.value.forEach(m => {
    const key = m.globalKey || ''
    list.push({
      _id:         `err-${key}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:         'error',
      key,
      integration:  m.integration || '',
      status:       m.status || 'ERROR',
      description:  m.statusDescription || '',
    })
  })

  // Add duplicate events from API
  console.log('Adding duplicate events:', duplicateEvents.value.length, 'items')
  duplicateEvents.value.forEach(m => {
    const key = m.globalKey || ''
    list.push({
      _id:         `dup-${key}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:         'duplicate',
      key,
      integration:  m.integration || '',
      status:       'DUPLICATE',
      description:  'Duplicate request blocked by idempotency service',
    })
  })

  console.log('Total messages by type:', {
    sent: list.filter(m => m.type === 'sent').length,
    received: list.filter(m => m.type === 'received').length,
    error: list.filter(m => m.type === 'error').length,
    duplicate: list.filter(m => m.type === 'duplicate').length,
  })

  allMessages.value = list
}

const rawFields = computed(() => {
  if (!selectedMsg.value) return []
  const raw = selectedMsg.value._raw || selectedMsg.value
  return Object.entries(raw)
    .filter(([k]) => !k.startsWith('_'))
    .map(([k, v]) => [k, v === null || v === undefined ? '—' : (typeof v === 'object' ? JSON.stringify(v, null, 2) : String(v))])
})

const filters = computed(() => {
  // Calculate totals from auditActivity for errors (time-limited)
  const auditArray = Array.isArray(auditActivity.value) ? auditActivity.value : []
  const errorCountFromAudit = auditArray.reduce((sum, slot) => {
    return sum +
      (slot['Некорректное входящее событие'] || 0) +
      (slot['Не найден маршрут для входящего события'] || 0) +
      (slot['Некорректный ответ от системы-получателя'] || 0) +
      (slot['Получен ответ без ожидающей операции'] || 0) +
      (slot['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
  }, 0)

  const totalCount = allMessages.value.length

  console.log('Filters computed:', {
    senderStats: senderStats.value,
    receiverStats: receiverStats.value,
    totalDuplicateCount: totalDuplicateCount.value,
    errorCountFromAudit,
    totalCount,
    allMessagesLength: allMessages.value.length,
    auditActivity: auditActivity.value
  })

  return [
    { id: 'all',       label: 'Все',         icon: '≡',  count: totalCount,                                                                 activeBg: 'var(--md-surface-3)',    activeColor: 'var(--md-on-surface)' },
    { id: 'sent',      label: 'Отправлены',  icon: '↑',  count: senderStats.value.totalSent,                                   activeBg: 'rgba(130,177,255,0.2)', activeColor: '#82b1ff' },
    { id: 'received',  label: 'Получены',    icon: '↓',  count: receiverStats.value.totalReceived,                             activeBg: 'rgba(109,213,140,0.2)', activeColor: 'var(--md-success)' },
    { id: 'error',     label: 'Операции с ошибкой', icon: '⚠',  count: errorCountFromAudit,                                                          activeBg: 'rgba(242,184,181,0.2)', activeColor: 'var(--md-error)' },
    { id: 'duplicate', label: 'Дубли',        icon: '♻',  count: totalDuplicateCount.value,                                                 activeBg: 'rgba(246,193,66,0.2)',  activeColor: '#f6c142' },
  ]
})

const displayedMessages = computed(() => {
  let list = allMessages.value
  if (activeFilter.value !== 'all') list = list.filter(m => m.type === activeFilter.value)
  if (search.value.trim()) {
    const q = search.value.trim().toLowerCase()
    list = list.filter(m =>
      m.key?.toLowerCase().includes(q) ||
      m.integration?.toLowerCase().includes(q) ||
      m.status?.toLowerCase().includes(q)
    )
  }
  if (sortBy.value) {
    const col = sortBy.value
    list = [...list].sort((a, b) => {
      const va = String(a[col] ?? '').toLowerCase()
      const vb = String(b[col] ?? '').toLowerCase()
      return sortDir.value === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
    })
  }
  return list.slice(0, displayLimit.value)
})

function typeStyle(type) {
  return {
    sent:      { icon: '↑', label: 'Отправлено', color: '#82b1ff',           bg: 'rgba(130,177,255,0.12)' },
    received:  { icon: '↓', label: 'Получено',   color: 'var(--md-success)', bg: 'rgba(109,213,140,0.12)' },
    error:     { icon: '⚠', label: 'Ошибка',     color: 'var(--md-error)',   bg: 'rgba(242,184,181,0.12)' },
    duplicate: { icon: '♻', label: 'Дубль',      color: '#f6c142',           bg: 'rgba(246,193,66,0.12)'  },
  }[type] ?? { icon: '?', label: type, color: 'var(--md-on-surface-v)', bg: 'var(--md-surface-3)' }
}

function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, 10) + '…' + s.slice(-6) : s
}

function copy(key) {
  navigator.clipboard.writeText(key)
  toast.info('Скопировано')
}

async function loadAll() {
  loading.value = true
  try {
    const [sentRes, recvRes, errRes, dupRes, auditRes, statsRes, recvStatsRes] = await Promise.allSettled([
      senderApi.getSentMessages(),
      receiverApi.getReceivedEvents(),
      coreApi.getErrorEvents({ page: 0, limit: 100, sort: 'desc' }),
      coreApi.getDuplicateEvents(),
      loadAuditActivity(),
      senderApi.getStats(),
      receiverApi.getStats(),
    ])
    if (sentRes.status === 'fulfilled') {
      const d = sentRes.value.data?.data
      sentMessages.value = Array.isArray(d) ? d : (d ? [d] : [])
    }
    if (recvRes.status === 'fulfilled') {
      const d = recvRes.value.data?.data
      receivedEvents.value = Array.isArray(d) ? d : (d ? [d] : [])
    }
    if (errRes.status === 'fulfilled') {
      const d = errRes.value.data?.data
      errorEvents.value = d?.content ?? []
    }
    if (dupRes.status === 'fulfilled') {
      const d = dupRes.value.data?.data
      duplicateEvents.value = d?.content ?? []
      console.log('Duplicate events loaded:', duplicateEvents.value.length, 'items')
    } else {
      console.log('Duplicate events failed:', dupRes.reason)
    }
    if (statsRes.status === 'fulfilled') {
      const d = statsRes.value.data?.data
      senderStats.value = { totalSent: d?.totalSent || 0, totalReplies: d?.totalReplies || 0 }
      console.log('Sender stats response:', statsRes.value.data)
      console.log('Sender stats parsed:', senderStats.value)
    } else {
      console.log('Sender stats failed:', statsRes.reason)
    }
    if (recvStatsRes.status === 'fulfilled') {
      const d = recvStatsRes.value.data?.data
      receiverStats.value = { totalReceived: d?.totalReceived || 0, totalDuplicates: d?.totalDuplicates || 0 }
      totalDuplicateCount.value = d?.totalDuplicates || 0
      console.log('Receiver stats response:', recvStatsRes.value.data)
      console.log('Receiver stats parsed:', receiverStats.value)
      console.log('Total duplicates:', totalDuplicateCount.value)
    } else {
      console.log('Receiver stats failed:', recvStatsRes.reason)
    }
    // Build unified message list only after all data received
    rebuildMessages()
    lastRefresh.value = new Date().toLocaleTimeString('ru-RU')
    hasLoadedOnce.value = true
  } finally {
    loading.value = false
  }
}

async function retryEvent(key) {
  retryingKey.value = key
  try {
    const res = await coreApi.restartEvent(key)
    if (res.data?.success) {
      toast.success('Поставлено на повтор')
      await loadAll()
    } else {
      toast.error('Ошибка: ' + (res.data?.error?.text || 'неизвестно'))
    }
  } catch (e) {
    toast.error('Ошибка: ' + (e.response?.data?.error?.text || e.message))
  } finally {
    retryingKey.value = null
  }
}

let timer = null
watch(autoRefresh, (v) => {
  if (v) timer = setInterval(loadAll, 30000)
  else clearInterval(timer)
}, { immediate: true })

onMounted(loadAll)
onUnmounted(() => clearInterval(timer))
</script>

<style scoped>
</style>
