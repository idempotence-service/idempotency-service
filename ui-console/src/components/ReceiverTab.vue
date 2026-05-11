<template>
  <div class="space-y-5">
    <div>
      <h2 class="text-2xl font-semibold" style="color:var(--md-on-surface)">Receiver Simulator</h2>
      <p class="text-sm mt-1" style="color:var(--md-on-surface-v)">Порт <span class="mono">18082</span> · Управление режимом и ручной ответ</p>
    </div>

    <div class="grid grid-cols-1 lg:grid-cols-2 gap-5">
      <!-- Left column: controls -->
      <div class="space-y-5">
        <!-- Set Mode -->
        <div class="card p-5 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-medium" style="color:var(--md-on-surface)">Режим обработки</h3>
            <span class="text-xs mono" style="color:var(--md-on-surface-v)">POST /api/receiver/mode</span>
          </div>

          <div>
            <label class="label">Интеграция <span style="color:var(--md-error)">*</span></label>
            <input v-model="modeForm.integration" type="text" class="input" placeholder="system1-to-system2" />
          </div>
          <div>
            <label class="label">Режим <span style="color:var(--md-error)">*</span></label>
            <select v-model="modeForm.mode" class="select">
              <option value="AUTO_SUCCESS">AUTO_SUCCESS — автоматический успех</option>
              <option value="AUTO_FAIL_RESEND">AUTO_FAIL_RESEND — ошибка + повтор</option>
              <option value="AUTO_FAIL_NO_RESEND">AUTO_FAIL_NO_RESEND — ошибка без повтора</option>
              <option value="MANUAL">MANUAL — ручной ответ</option>
            </select>
          </div>

          <div class="px-3 py-2 rounded-xl text-xs leading-relaxed" style="background:var(--md-surface-3); color:var(--md-on-surface-v)">
            <span class="font-medium" style="color:var(--md-on-surface)">{{ modeForm.mode }}</span> —
            <span>{{ modeDescriptions[modeForm.mode] }}</span>
          </div>

          <button
            @click="doSetMode"
            :disabled="settingMode || !modeForm.integration.trim()"
            class="btn-primary w-full justify-center"
          >
            <svg v-if="settingMode" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ settingMode ? 'Применение...' : 'Установить режим' }}
          </button>
        </div>

        <!-- Manual Reply -->
        <div class="card p-5 space-y-4">
          <div class="flex items-center justify-between">
            <h3 class="font-medium" style="color:var(--md-on-surface)">Ручной ответ</h3>
            <span class="text-xs mono" style="color:var(--md-on-surface-v)">POST /api/receiver/reply</span>
          </div>
          <p class="text-xs" style="color:var(--md-on-surface-v)">Используется только когда режим <code class="mono" style="color:var(--md-primary)">MANUAL</code></p>

          <div>
            <label class="label">Интеграция <span style="color:var(--md-error)">*</span></label>
            <input v-model="replyForm.integration" type="text" class="input" placeholder="system1-to-system2" />
          </div>
          <div>
            <label class="label">Global Key <span style="color:var(--md-error)">*</span></label>
            <input v-model="replyForm.globalKey" type="text" class="input mono text-xs" placeholder="550e8400-e29b-41d4-..." />
          </div>
          <div>
            <label class="label">Результат <span style="color:var(--md-error)">*</span></label>
            <input v-model="replyForm.result" type="text" class="input" placeholder="SUCCESS / FAIL" />
          </div>
          <div>
            <label class="label">Описание результата</label>
            <input v-model="replyForm.resultDescription" type="text" class="input" placeholder="Платёж проведён успешно" />
          </div>
          <div class="flex items-center gap-3">
            <input
              id="needResend"
              v-model="replyForm.needResend"
              type="checkbox"
              class="w-4 h-4 rounded border-2"
              style="background:var(--md-surface-3); border-color:var(--md-outline); accent-color:var(--md-primary)"
            />
            <label for="needResend" class="text-sm" style="color:var(--md-on-surface)">Запросить повтор (needResend)</label>
          </div>

          <button
            @click="doManualReply"
            :disabled="sendingReply || !replyForm.integration.trim() || !replyForm.globalKey.trim() || !replyForm.result.trim()"
            class="btn-success w-full justify-center"
          >
            <svg v-if="sendingReply" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ sendingReply ? 'Отправка...' : '↩ Отправить ответ' }}
          </button>
        </div>
      </div>

      <!-- Right column: received events -->
      <div class="card p-5 flex flex-col">
        <div class="flex items-center justify-between mb-4">
          <h3 class="font-medium" style="color:var(--md-on-surface)">Принятые события</h3>
          <div class="flex items-center gap-2">
            <button @click="loadEvents" :disabled="loadingEvents" class="btn-tonal btn-sm">
              <svg class="w-4 h-4" :class="{ 'animate-spin': loadingEvents }" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                  d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15" />
              </svg>
              Обновить
            </button>
            <button @click="doResetReceiver" class="btn-danger btn-sm">
              Сбросить
            </button>
          </div>
        </div>

        <p class="text-sm mb-3" style="color:var(--md-on-surface-v)">
          Всего: <strong style="color:var(--md-primary)">{{ receivedEvents.length }}</strong>
        </p>

        <div class="table-container flex-1 overflow-y-auto max-h-[500px]">
          <table class="data-table" v-if="receivedEvents.length">
            <thead>
              <tr>
                <th>Global Key</th>
                <th>Интеграция</th>
                <th>Статус</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="(ev, i) in receivedEvents"
                :key="i"
                @click="fillReplyFromEvent(ev)"
                class="!cursor-pointer"
                title="Нажмите, чтобы заполнить форму ответа"
              >
                <td class="mono text-xs">{{ truncate(ev.globalKey || JSON.stringify(ev), 24) }}</td>
                <td class="text-xs text-slate-400">{{ ev.integration || '—' }}</td>
                <td><StatusBadge :status="ev.status || ev.result || '?'" /></td>
              </tr>
            </tbody>
          </table>
          <p v-else class="text-center text-slate-600 py-10 text-sm">Принятых событий нет</p>
        </div>

        <p class="text-xs text-slate-600 mt-2">
          * Нажмите на строку, чтобы заполнить форму ручного ответа
        </p>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { receiverApi } from '../api/receiver.js'
import { useToastStore } from '../stores/toast.js'
import StatusBadge from './StatusBadge.vue'

const toast = useToastStore()

const modeForm = ref({ integration: 'system1-to-system2', mode: 'AUTO_SUCCESS' })
const settingMode = ref(false)

const replyForm = ref({
  integration: 'system1-to-system2',
  globalKey: '',
  result: 'SUCCESS',
  resultDescription: '',
  needResend: false,
})
const sendingReply = ref(false)

const receivedEvents = ref([])
const loadingEvents = ref(false)

const modeDescriptions = {
  AUTO_SUCCESS: 'Автоматически отвечает успехом на каждое входящее событие',
  AUTO_FAIL_RESEND: 'Автоматически отвечает ошибкой и запрашивает повтор',
  AUTO_FAIL_NO_RESEND: 'Автоматически отвечает ошибкой без запроса повтора',
  MANUAL: 'Ожидает ручного ответа через форму ниже',
}

function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, n) + '…' : s
}

function fillReplyFromEvent(ev) {
  if (ev.globalKey) replyForm.value.globalKey = ev.globalKey
  if (ev.integration) replyForm.value.integration = ev.integration
  toast.info('Форма ответа заполнена')
}

async function doSetMode() {
  settingMode.value = true
  try {
    const res = await receiverApi.setMode({ integration: modeForm.value.integration, mode: modeForm.value.mode })
    if (res.data?.success) {
      toast.success(`Режим ${modeForm.value.mode} установлен`)
    } else {
      toast.error('Ошибка: ' + (res.data?.error?.text || 'неизвестно'))
    }
  } catch (e) {
    toast.error('Ошибка установки режима: ' + e.message)
  } finally {
    settingMode.value = false
  }
}

async function doManualReply() {
  sendingReply.value = true
  try {
    const body = {
      integration: replyForm.value.integration,
      globalKey: replyForm.value.globalKey,
      result: replyForm.value.result,
      needResend: replyForm.value.needResend,
      ...(replyForm.value.resultDescription.trim() && {
        resultDescription: replyForm.value.resultDescription,
      }),
    }
    const res = await receiverApi.sendManualReply(body)
    if (res.data?.success) {
      toast.success('Ответ отправлен')
      replyForm.value.globalKey = ''
      replyForm.value.resultDescription = ''
    } else {
      toast.error('Ошибка: ' + (res.data?.error?.text || 'неизвестно'))
    }
  } catch (e) {
    toast.error('Ошибка отправки ответа: ' + e.message)
  } finally {
    sendingReply.value = false
  }
}

async function loadEvents() {
  loadingEvents.value = true
  try {
    const res = await receiverApi.getReceivedEvents()
    const d = res.data?.data
    receivedEvents.value = Array.isArray(d) ? d : (d ? [d] : [])
  } catch (e) {
    toast.error('Ошибка загрузки событий: ' + e.message)
  } finally {
    loadingEvents.value = false
  }
}

async function doResetReceiver() {
  try {
    await receiverApi.resetState()
    receivedEvents.value = []
    toast.success('Состояние receiver сброшено')
  } catch (e) {
    toast.error('Ошибка сброса: ' + e.message)
  }
}

onMounted(loadEvents)
</script>
