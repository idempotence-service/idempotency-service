<template>
  <div class="min-h-screen flex items-center justify-center p-4" style="background:var(--md-bg)">
    <div class="w-full max-w-sm">

      <!-- Logo -->
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-3xl mb-5"
          style="background:var(--md-primary-c)">
          <svg class="w-8 h-8" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
        </div>
        <h1 class="text-2xl font-semibold tracking-tight" style="color:var(--md-on-surface)">Idempotency Console</h1>
        <p class="text-sm mt-2" style="color:var(--md-on-surface-v)">Управление событиями и мониторинг</p>
      </div>

      <!-- Card -->
      <div class="card p-7">
        <form @submit.prevent="handleLogin" class="space-y-5">
          <div>
            <label class="label">API Token</label>
            <div class="relative">
              <input
                v-model="token"
                :type="showPassword ? 'text' : 'password'"
                class="input mono pr-10"
                placeholder="Введите токен доступа..."
                autocomplete="current-password"
                :disabled="loading"
              />
              <button
                type="button"
                @click="showPassword = !showPassword"
                class="absolute right-3 top-1/2 -translate-y-1/2 p-1.5 rounded-lg transition-colors hover:opacity-80"
                style="color:var(--md-on-surface-v)"
                :title="showPassword ? 'Скрыть' : 'Показать'"
              >
                <svg v-if="showPassword" class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z"/>
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z"/>
                </svg>
                <svg v-else class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13.875 18.825A10.05 10.05 0 0112 19c-4.478 0-8.268-2.943-9.543-7a9.97 9.97 0 011.563-3.029m5.858.908a3 3 0 114.243 4.243M9.878 9.878l4.242 4.242M9.88 9.88l-3.29-3.29m7.532 7.532l3.29 3.29M3 3l3.59 3.59m0 0A9.953 9.953 0 0112 5c4.478 0 8.268 2.943 9.543 7a10.025 10.025 0 01-4.132 5.411m0 0L21 21"/>
                </svg>
              </button>
            </div>
          </div>

          <div v-if="error"
            class="px-4 py-3 rounded-xl text-sm"
            style="background:rgba(242,184,181,0.1); border:1px solid rgba(242,184,181,0.2); color:var(--md-error)">
            {{ error }}
          </div>

          <button
            type="submit"
            class="btn-primary w-full justify-center"
            style="padding:12px 24px; border-radius:12px"
            :disabled="loading || !token.trim()"
          >
            <svg v-if="loading" class="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"/>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/>
            </svg>
            {{ loading ? 'Проверка...' : 'Войти' }}
          </button>
        </form>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import { coreApi } from '../api/core.js'

const router = useRouter()
const authStore = useAuthStore()

const token = ref('operator-token')
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')

async function handleLogin() {
  if (!token.value.trim()) return
  loading.value = true
  error.value = ''

  authStore.login(token.value.trim())

  try {
    const res = await coreApi.getErrorEvents({ page: 0, limit: 1 })
    if (res.data?.success === false) {
      authStore.logout()
      error.value = 'Неверный токен или нет доступа'
    } else {
      router.push('/dashboard')
    }
  } catch (e) {
    authStore.logout()
    error.value = e.response?.data?.error?.text || 'Ошибка подключения к сервису'
  } finally {
    loading.value = false
  }
}
</script>
