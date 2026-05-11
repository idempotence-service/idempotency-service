<template>
  <div class="space-y-5">

    <!-- Header row -->
    <div class="flex items-start justify-between gap-4">
      <div>
        <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">События с ошибками</h2>
        <p class="text-sm mt-1 mono" style="color:var(--md-on-surface-v)">
          GET /get-error-events ·
          <span v-if="totalElements > 0">
            <span style="color:var(--md-error)">{{ totalElements }}</span> событий
          </span>
          <span v-else>загрузка...</span>
        </p>
      </div>
      <button @click="reload" :disabled="loading" class="btn-tonal shrink-0">
        <svg class="w-4 h-4" :class="{ 'animate-spin': loading }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
            d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"/>
        </svg>
        Обновить
      </button>
    </div>

    <!-- Filter bar -->
    <div class="card p-4 flex flex-wrap items-center gap-3">

      <!-- Search -->
      <div class="relative flex-1 min-w-48">
        <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 pointer-events-none" style="color:var(--md-outline)"
          fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0"/>
        </svg>
        <input
          v-model="filterKey"
          type="text"
          class="input pl-9"
          placeholder="Поиск по globalKey..."
        />
      </div>

      <!-- Integration filter -->
      <div class="min-w-36">
        <input v-model="filterIntegration" type="text" class="input" placeholder="Интеграция..." />
      </div>

      <!-- Sort toggle -->
      <div class="flex items-center gap-1 px-1">
        <span class="text-xs font-medium" style="color:var(--md-on-surface-v)">Сортировка:</span>
        <button
          @click="toggleSort"
          class="flex items-center gap-1.5 px-3 py-2 rounded-full text-sm font-medium transition-all duration-200"
          style="background:var(--md-surface-2); border:1px solid var(--md-outline-v); color:var(--md-primary)"
          :title="sort === 'desc' ? 'Сначала новые — нажмите для переключения' : 'Сначала старые — нажмите для переключения'"
        >
          <svg class="w-3.5 h-3.5 transition-transform duration-300" :class="{ 'rotate-180': sort === 'asc' }"
            fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M19 9l-7 7-7-7"/>
          </svg>
          {{ sort === 'desc' ? 'Новые' : 'Старые' }}
        </button>
      </div>

      <!-- Page size -->
      <div class="flex items-center gap-2">
        <span class="text-xs shrink-0" style="color:var(--md-on-surface-v)">По</span>
        <div class="flex items-center gap-1 p-1 rounded-full" style="background:var(--md-surface-2); border:1px solid var(--md-outline-v)">
          <button
            v-for="n in [10, 20, 50]" :key="n"
            @click="limit = n"
            class="px-3 py-1 rounded-full text-sm font-medium transition-all duration-150"
            :style="limit === n
              ? 'background:var(--md-primary-c); color:var(--md-primary)'
              : 'color:var(--md-on-surface-v)'"
          >{{ n }}</button>
        </div>
      </div>
    </div>

    <!-- Table card -->
    <div class="card overflow-hidden">

      <!-- Loading overlay -->
      <div v-if="loading && !events.length"
        class="flex items-center justify-center py-20 gap-3 text-sm"
        style="color:var(--md-on-surface-v)">
        <svg class="w-5 h-5 animate-spin" fill="none" viewBox="0 0 24 24">
          <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
          <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
        </svg>
        Загрузка событий...
      </div>

      <!-- Empty -->
      <div v-else-if="!filteredEvents.length && !loading"
        class="flex flex-col items-center justify-center py-20 gap-3"
        style="color:var(--md-on-surface-v)">
        <svg class="w-12 h-12 opacity-20" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5"
            d="M9 12l2 2 4-4m6 2a9 9 0 11-18 0 9 9 0 0118 0z"/>
        </svg>
        <p class="text-sm">Событий с ошибками не найдено</p>
      </div>

      <!-- Data table -->
      <table v-else class="data-table">
        <thead>
          <tr>
            <th>Global Key</th>
            <th>Статус</th>
            <th>Сервис</th>
            <th>Интеграция</th>
            <th style="text-align:right">Действия</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="ev in filteredEvents"
            :key="ev.globalKey"
            @click="openDetail(ev)"
            class="group"
          >
            <td>
              <div class="flex items-center gap-2">
                <span class="mono text-xs" style="color:var(--md-on-surface)">{{ truncateKey(ev.globalKey) }}</span>
                <button
                  @click.stop="copyKey(ev.globalKey)"
                  class="opacity-0 group-hover:opacity-100 btn-icon !w-6 !h-6 shrink-0"
                  title="Копировать"
                  style="font-size:14px">⎘</button>
              </div>
            </td>
            <td @click.stop><StatusBadge :status="ev.status" /></td>
            <td class="text-xs" style="color:var(--md-on-surface-v)">{{ ev.service || '—' }}</td>
            <td class="text-xs mono" style="color:var(--md-on-surface-v)">{{ ev.integration }}</td>
            <td @click.stop>
              <div class="flex items-center justify-end gap-2">
                <button @click="openDetail(ev)" class="btn-tonal btn-sm">Детали</button>
                <button
                  @click="confirmRetry(ev)"
                  class="btn-danger btn-sm"
                  :disabled="retryingKey === ev.globalKey"
                >
                  <svg v-if="retryingKey === ev.globalKey" class="w-3 h-3 animate-spin" fill="none" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
                  </svg>
                  <span v-else>↻</span>
                  Ретрай
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>

      <!-- Pagination -->
      <div v-if="totalPages > 1"
        class="flex items-center justify-between px-4 py-3"
        style="border-top:1px solid var(--md-outline-v)">
        <span class="text-xs" style="color:var(--md-on-surface-v)">
          Стр. {{ page + 1 }} из {{ totalPages }} · {{ totalElements }} событий
        </span>
        <div class="flex items-center gap-1">
          <button @click="goPage(page - 1)" :disabled="page === 0" class="btn-icon">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7"/>
            </svg>
          </button>
          <template v-for="p in paginationPages" :key="p">
            <span v-if="p === '...'" class="w-8 text-center text-sm" style="color:var(--md-outline)">…</span>
            <button v-else @click="goPage(p)"
              class="w-8 h-8 rounded-full text-sm font-medium transition-all duration-150"
              :style="p === page
                ? 'background:var(--md-primary-c); color:var(--md-primary)'
                : 'color:var(--md-on-surface-v)'">
              {{ p + 1 }}
            </button>
          </template>
          <button @click="goPage(page + 1)" :disabled="page >= totalPages - 1" class="btn-icon">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7"/>
            </svg>
          </button>
        </div>
      </div>
    </div>

    <!-- Confirm Retry Dialog -->
    <Teleport to="body">
      <Transition name="modal">
        <div v-if="retryTarget" class="fixed inset-0 z-50 flex items-center justify-center p-4"
          @click.self="retryTarget = null">
          <div class="absolute inset-0 bg-black/50 backdrop-blur-sm"/>
          <div class="relative card p-6 w-full max-w-sm shadow-2xl" style="background:var(--md-surface-2)">
            <h3 class="text-base font-semibold mb-2" style="color:var(--md-on-surface)">Подтвердите перезапуск</h3>
            <p class="text-sm mb-2" style="color:var(--md-on-surface-v)">Событие будет поставлено в очередь повтора:</p>
            <div class="rounded-xl px-3 py-2 mb-5 break-all text-xs mono" style="background:var(--md-surface-3); color:var(--md-primary)">
              {{ retryTarget }}
            </div>
            <div class="flex gap-3 justify-end">
              <button @click="retryTarget = null" class="btn-tonal">Отмена</button>
              <button @click="doRetry" class="btn-danger">↻ Перезапустить</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>

    <!-- Detail Modal -->
    <EventDetailModal
      v-model="detailOpen"
      :event="selectedEvent"
      :retrying="retryingKey === selectedEvent?.globalKey"
      @retry="(key) => { detailOpen = false; confirmRetry({ globalKey: key }) }"
    />
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { coreApi } from '../api/core.js'
import { useToastStore } from '../stores/toast.js'
import StatusBadge from './StatusBadge.vue'
import EventDetailModal from './EventDetailModal.vue'

const toast = useToastStore()

const events = ref([])
const loading = ref(false)
const page = ref(0)
const limit = ref(20)
const sort = ref('desc')
const totalElements = ref(0)
const totalPages = ref(0)

const filterKey = ref('')
const filterIntegration = ref('')
const retryingKey = ref(null)
const retryTarget = ref(null)
const detailOpen = ref(false)
const selectedEvent = ref(null)

let debounceTimer = null
function debounce(fn, ms = 350) {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(fn, ms)
}

watch(filterKey, () => debounce(() => { page.value = 0; loadEvents() }))
watch(filterIntegration, () => debounce(() => { page.value = 0; loadEvents() }))
watch(limit, () => { page.value = 0; loadEvents() })

const filteredEvents = computed(() => {
  let list = events.value
  const qk = filterKey.value.trim().toLowerCase()
  const qi = filterIntegration.value.trim().toLowerCase()
  if (qk) list = list.filter(e => e.globalKey?.toLowerCase().includes(qk))
  if (qi) list = list.filter(e => e.integration?.toLowerCase().includes(qi))
  return list
})

const paginationPages = computed(() => {
  const total = totalPages.value
  const cur = page.value
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages = []
  if (cur > 2) pages.push(0, '...')
  for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) pages.push(i)
  if (cur < total - 3) pages.push('...', total - 1)
  return pages
})

async function loadEvents() {
  loading.value = true
  try {
    const res = await coreApi.getErrorEvents({ page: page.value, limit: limit.value, sort: sort.value })
    const data = res.data?.data
    events.value = data?.content ?? []
    totalElements.value = data?.totalElements ?? 0
    totalPages.value = data?.totalPages ?? 0
  } catch (e) {
    toast.error('Ошибка загрузки: ' + (e.response?.data?.error?.text || e.message))
  } finally {
    loading.value = false
  }
}

function reload() { page.value = 0; loadEvents() }

function toggleSort() {
  sort.value = sort.value === 'desc' ? 'asc' : 'desc'
  page.value = 0
  loadEvents()
}

function goPage(p) {
  if (p < 0 || p >= totalPages.value) return
  page.value = p
  loadEvents()
}

function truncateKey(key) {
  if (!key) return '—'
  return key.length > 26 ? key.slice(0, 8) + '…' + key.slice(-8) : key
}

function copyKey(key) {
  navigator.clipboard.writeText(key)
  toast.info('globalKey скопирован')
}

function openDetail(ev) {
  selectedEvent.value = ev
  detailOpen.value = true
}

function confirmRetry(ev) {
  retryTarget.value = ev.globalKey
}

async function doRetry() {
  const key = retryTarget.value
  retryTarget.value = null
  retryingKey.value = key
  try {
    const res = await coreApi.restartEvent(key)
    if (res.data?.success) {
      toast.success('Событие поставлено на повтор')
      await loadEvents()
    } else {
      toast.error('Ошибка: ' + (res.data?.error?.text || 'неизвестно'))
    }
  } catch (e) {
    toast.error('Ошибка перезапуска: ' + (e.response?.data?.error?.text || e.message))
  } finally {
    retryingKey.value = null
  }
}

onMounted(loadEvents)
</script>

<style scoped>
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s ease; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>
