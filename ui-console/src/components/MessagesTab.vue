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
      <div v-if="isInitialLoadingState" class="absolute inset-0 z-10" style="background:var(--md-surface); min-height:300px">
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
      <div v-if="!isInitialLoadingState && !displayedMessages.length && hasLoadedOnce"
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
          <col style="width:200px">
          <col style="width:140px">
          <col style="width:180px">
          <col style="width:150px">
        </colgroup>
        <thead>
          <tr style="height:48px">
            <th @click="toggleSortLocal('type')" class="cursor-pointer select-none py-3">
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
            <th @click="toggleSortLocal('key')" class="cursor-pointer select-none py-3">
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
            <th @click="toggleSortLocal('integration')" class="cursor-pointer select-none py-3">
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
            <th @click="toggleSortLocal('timestamp')" class="cursor-pointer select-none py-3">
              <div class="flex items-center gap-2 h-full">
                Время
                <svg v-if="sortBy==='timestamp'" class="w-3.5 h-3.5" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
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
                <button v-if="msg.key" @click.stop="copyLocal(msg.key)"
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
            <!-- Time -->
            <td class="py-3">
              <div class="flex items-center h-full">
                <span class="text-xs mono" style="color:var(--md-on-surface-v)">
                  {{ formatTimestamp(msg.timestamp) }}
                </span>
              </div>
            </td>
            <!-- Description -->
            <td class="py-3">
              <div class="flex items-center h-full">
                <span 
                  class="text-xs cursor-help" 
                  style="color:var(--md-on-surface-v)"
                  :title="msg.description || ''">
                  {{ msg.description ? truncate(msg.description, 30) : '—' }}
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
import { toggleSort, typeStyle, truncate, formatTimestamp, copy, rebuildMessages, filterAndSortMessages, formatRawFields, calculateErrorCountFromAudit, buildFilters, isInitialLoading, isRefreshing, shouldLoadMore, parseApiResponse, calculateAuditActivitySince, extractErrorMessage } from '../utils/messagesHelpers.js'

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

const sortBy  = ref('timestamp')
const sortDir = ref('desc')
const selectedMsg = ref(null)
const allMessages = ref([])
const scrollContainer = ref(null)

// Loading states - no flash on refresh
const isInitialLoadingState = computed(() => isInitialLoading(loading.value, allMessages.value.length))
const isRefreshingState = computed(() => isRefreshing(loading.value, allMessages.value.length))

function toggleSortLocal(col) {
  toggleSort(sortBy, sortDir, col)
}

function handleScroll() {
  if (shouldLoadMore(scrollContainer.value, loadingMore.value, displayLimit.value, allMessages.value.length)) {
    loadingMore.value = true
    setTimeout(() => {
      displayLimit.value = Math.min(displayLimit.value + 100, allMessages.value.length)
      loadingMore.value = false
    }, 100)
  }
}

async function loadAuditActivity() {
  try {
    const since = calculateAuditActivitySince()
    const res = await coreApi.getAuditActivity(since)
    auditActivity.value = res.data?.data || []
  } catch {
    auditActivity.value = []
  }
}

function rebuildMessagesLocal() {
  allMessages.value = rebuildMessages(sentMessages.value, receivedEvents.value, errorEvents.value, duplicateEvents.value)
}

const rawFields = computed(() => {
  return formatRawFields(selectedMsg.value)
})

const filters = computed(() => {
  return buildFilters(auditActivity.value, senderStats.value, receiverStats.value, totalDuplicateCount.value)
})

const displayedMessages = computed(() => {
  return filterAndSortMessages(allMessages.value, activeFilter.value, search.value, sortBy.value, sortDir.value, displayLimit.value)
})

function copyLocal(key) {
  copy(key, toast)
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
      sentMessages.value = parseApiResponse(d)
    }
    if (recvRes.status === 'fulfilled') {
      const d = recvRes.value.data?.data
      receivedEvents.value = parseApiResponse(d)
    }
    if (errRes.status === 'fulfilled') {
      const d = errRes.value.data?.data
      errorEvents.value = d?.content ?? []
    }
    if (dupRes.status === 'fulfilled') {
      const d = dupRes.value.data?.data
      duplicateEvents.value = d?.content ?? []
      totalDuplicateCount.value = d?.totalElements ?? duplicateEvents.value.length
    }
    if (statsRes.status === 'fulfilled') {
      const d = statsRes.value.data?.data
      senderStats.value = { totalSent: d?.totalSent || 0, totalReplies: d?.totalReplies || 0 }
    }
    if (recvStatsRes.status === 'fulfilled') {
      const d = recvStatsRes.value.data?.data
      receiverStats.value = { totalReceived: d?.totalReceived || 0, totalDuplicates: d?.totalDuplicates || 0 }
    }
    // Build unified message list only after all data received
    rebuildMessagesLocal()
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
    toast.error('Ошибка: ' + extractErrorMessage(e))
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
