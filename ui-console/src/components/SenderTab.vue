<template>
  <div class="space-y-5">
    <div>
      <h2 class="text-lg font-semibold text-slate-100">Sender Simulator</h2>
      <p class="text-sm text-slate-500 mt-0.5">Порт <span class="mono">18081</span> · Отправка событий и просмотр состояния</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <!-- Send Event Form -->
      <div class="card p-5 space-y-4">
        <div class="flex items-center justify-between">
          <h3 class="font-medium text-slate-200">Отправить событие</h3>
          <span class="text-xs text-slate-600 mono">POST /api/sender/send</span>
        </div>

        <div>
          <label class="label">Интеграция <span class="text-red-400">*</span></label>
          <input v-model="sendForm.integration" type="text" class="input" placeholder="system1-to-system2" />
        </div>
        <div>
          <label class="label">UID (необязательно)</label>
          <input v-model="sendForm.uid" type="text" class="input mono" placeholder="Авто-генерация если пусто" />
        </div>
        <div>
          <label class="label">Количество дублей</label>
          <input v-model.number="sendForm.duplicates" type="number" min="1" max="10" class="input w-24" />
          <p class="text-xs text-slate-600 mt-1">Для теста идемпотентности — отправьте >1</p>
        </div>
        <div>
          <label class="label">Payload (JSON)</label>
          <textarea
            v-model="sendForm.payloadRaw"
            class="input font-mono text-xs min-h-[80px] resize-y"
            placeholder='{"amount": 1000, "currency": "RUB"}'
          />
          <p v-if="payloadError" class="text-xs text-red-400 mt-1">{{ payloadError }}</p>
        </div>
        <div>
          <label class="label">Headers (JSON)</label>
          <textarea
            v-model="sendForm.headersRaw"
            class="input font-mono text-xs min-h-[60px] resize-y"
            placeholder='{"X-Trace-Id": "trace-123"}'
          />
          <p v-if="headersError" class="text-xs text-red-400 mt-1">{{ headersError }}</p>
        </div>

        <button
          @click="doSend"
          :disabled="sending || !sendForm.integration.trim()"
          class="btn-primary w-full justify-center py-2"
        >
          <svg v-if="sending" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
          </svg>
          {{ sending ? 'Отправка...' : '📤 Отправить' }}
        </button>

        <div v-if="sendResult" class="px-3 py-2 rounded-md text-sm"
          :class="sendResult.success ? 'bg-emerald-500/10 border border-emerald-500/20 text-emerald-300' : 'bg-red-500/10 border border-red-500/20 text-red-300'"
        >
          <span v-if="sendResult.success">✓ Отправлено · UID: <code class="mono text-xs">{{ sendResult.data }}</code></span>
          <span v-else>✕ {{ sendResult.error?.text || 'Ошибка' }}</span>
        </div>
      </div>

      <!-- Reset + Actions -->
      <div class="space-y-5">
        <!-- Sent Messages -->
        <div class="card p-5">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-medium text-slate-200">Отправленные сообщения</h3>
            <div class="flex gap-2">
              <button @click="loadSent" :disabled="loadingSent" class="btn-ghost btn-sm">
                <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': loadingSent }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                    d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
                </svg>
                Обновить
              </button>
              <button @click="doResetSender" class="btn-ghost btn-sm text-red-400 border-red-500/30 hover:bg-red-500/10">
                Сбросить
              </button>
            </div>
          </div>
          <div class="table-container max-h-48">
            <table class="data-table" v-if="sentMessages.length">
              <thead><tr><th>UID</th><th>Интеграция</th></tr></thead>
              <tbody>
                <tr v-for="(msg, i) in sentMessages" :key="i" class="!cursor-default">
                  <td class="mono text-xs">{{ truncate(msg.uid || msg.id || JSON.stringify(msg), 28) }}</td>
                  <td class="text-xs text-slate-400">{{ msg.integration || '—' }}</td>
                </tr>
              </tbody>
            </table>
            <p v-else class="text-center text-slate-600 py-6 text-sm">Нет отправленных сообщений</p>
          </div>
        </div>

        <!-- Replies -->
        <div class="card p-5">
          <div class="flex items-center justify-between mb-3">
            <h3 class="font-medium text-slate-200">Полученные ответы</h3>
            <button @click="loadReplies" :disabled="loadingReplies" class="btn-ghost btn-sm">
              <svg class="w-3.5 h-3.5" :class="{ 'animate-spin': loadingReplies }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Обновить
            </button>
          </div>
          <div class="table-container max-h-48">
            <table class="data-table" v-if="replies.length">
              <thead><tr><th>Global Key</th><th>Результат</th></tr></thead>
              <tbody>
                <tr v-for="(r, i) in replies" :key="i" class="!cursor-default">
                  <td class="mono text-xs">{{ truncate(r.globalKey || JSON.stringify(r), 28) }}</td>
                  <td><StatusBadge :status="r.result || r.status || '?'" /></td>
                </tr>
              </tbody>
            </table>
            <p v-else class="text-center text-slate-600 py-6 text-sm">Ответов нет</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { senderApi } from '../api/sender.js'
import { useToastStore } from '../stores/toast.js'
import StatusBadge from './StatusBadge.vue'

const toast = useToastStore()

const sendForm = ref({
  integration: 'system1-to-system2',
  uid: '',
  duplicates: 1,
  payloadRaw: '{\n  "amount": 1000,\n  "currency": "RUB"\n}',
  headersRaw: '',
})
const sending = ref(false)
const sendResult = ref(null)
const payloadError = ref('')
const headersError = ref('')

const sentMessages = ref([])
const replies = ref([])
const loadingSent = ref(false)
const loadingReplies = ref(false)

function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, n) + '…' : s
}

function parseJsonField(raw, errorRef) {
  errorRef.value = ''
  if (!raw.trim()) return null
  try {
    return JSON.parse(raw)
  } catch {
    errorRef.value = 'Невалидный JSON'
    return undefined
  }
}

async function doSend() {
  const payload = parseJsonField(sendForm.value.payloadRaw, payloadError)
  const headers = parseJsonField(sendForm.value.headersRaw, headersError)
  if (payload === undefined || headers === undefined) return

  sending.value = true
  sendResult.value = null
  try {
    const body = {
      integration: sendForm.value.integration,
      duplicates: sendForm.value.duplicates,
      ...(sendForm.value.uid.trim() && { uid: sendForm.value.uid.trim() }),
      ...(payload !== null && { payload }),
      ...(headers !== null && { headers }),
    }
    const res = await senderApi.sendEvent(body)
    sendResult.value = res.data
    if (res.data?.success) {
      toast.success('Событие отправлено · UID: ' + res.data.data)
      loadSent()
    } else {
      toast.error('Ошибка отправки: ' + (res.data?.error?.text || 'неизвестно'))
    }
  } catch (e) {
    sendResult.value = { success: false, error: { text: e.message } }
    toast.error('Ошибка: ' + e.message)
  } finally {
    sending.value = false
  }
}

async function loadSent() {
  loadingSent.value = true
  try {
    const res = await senderApi.getSentMessages()
    const d = res.data?.data
    sentMessages.value = Array.isArray(d) ? d : (d ? [d] : [])
  } catch (e) {
    toast.error('Ошибка загрузки отправленных: ' + e.message)
  } finally {
    loadingSent.value = false
  }
}

async function loadReplies() {
  loadingReplies.value = true
  try {
    const res = await senderApi.getReplies()
    const d = res.data?.data
    replies.value = Array.isArray(d) ? d : (d ? [d] : [])
  } catch (e) {
    toast.error('Ошибка загрузки ответов: ' + e.message)
  } finally {
    loadingReplies.value = false
  }
}

async function doResetSender() {
  try {
    await senderApi.resetState()
    sentMessages.value = []
    replies.value = []
    toast.success('Состояние sender сброшено')
  } catch (e) {
    toast.error('Ошибка сброса: ' + e.message)
  }
}

onMounted(() => {
  loadSent()
  loadReplies()
})
</script>
