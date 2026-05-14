<template>
  <div class="min-h-screen flex flex-col" style="background:var(--md-bg)">

    <!-- Top App Bar (MD3) -->
    <header class="h-16 flex items-center px-6 gap-6 shrink-0 sticky top-0 z-40"
      style="background:var(--md-surface); border-bottom:1px solid var(--md-outline-v); box-shadow:0 1px 8px rgba(0,0,0,0.35)">

      <!-- Brand -->
      <div class="flex items-center gap-3 shrink-0">
        <div class="w-8 h-8 rounded-xl flex items-center justify-center"
          style="background:var(--md-primary-c)">
          <svg class="w-4 h-4" style="color:var(--md-primary)" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
              d="M9 12l2 2 4-4m5.618-4.016A11.955 11.955 0 0112 2.944a11.955 11.955 0 01-8.618 3.04A12.02 12.02 0 003 9c0 5.591 3.824 10.29 9 11.622 5.176-1.332 9-6.03 9-11.622 0-1.042-.133-2.052-.382-3.016z"/>
          </svg>
        </div>
        <span class="font-semibold text-base" style="color:var(--md-on-surface)">Idempotency Console</span>
      </div>

      <!-- Tab navigation -->
      <nav class="flex items-center gap-1 flex-1">
        <!-- Overview -->
        <button @click="activeTab = 'overview'"
          class="flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200"
          :style="activeTab === 'overview' ? 'background:rgba(208,188,255,0.15); color:var(--md-primary)' : 'color:var(--md-on-surface-v)'">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z"/></svg>
          <span>Обзор</span>
        </button>
        <!-- Messages -->
        <button @click="activeTab = 'messages'"
          class="flex items-center gap-2 px-4 py-2 rounded-full text-sm font-medium transition-all duration-200"
          :style="activeTab === 'messages' ? 'background:rgba(208,188,255,0.15); color:var(--md-primary)' : 'color:var(--md-on-surface-v)'">
          <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M8 10h.01M12 10h.01M16 10h.01M9 16H5a2 2 0 01-2-2V6a2 2 0 012-2h14a2 2 0 012 2v8a2 2 0 01-2 2h-5l-5 5v-5z"/></svg>
          <span>Сообщения</span>
        </button>
      </nav>

      <!-- Right side -->
      <div class="flex items-center gap-3">
        <div class="hidden sm:flex items-center gap-2 px-3 py-1.5 rounded-full"
          style="background:var(--md-surface-2); border:1px solid var(--md-outline-v)">
          <div class="w-1.5 h-1.5 rounded-full bg-green-400 animate-pulse"></div>
          <span class="text-xs mono" style="color:var(--md-on-surface-v)">{{ authStore.token }}</span>
        </div>
        <button @click="handleLogout" class="btn-tonal btn-sm">Выйти</button>
      </div>
    </header>

    <!-- Content -->
    <main class="flex-1 overflow-auto p-6 max-w-[1400px] mx-auto w-full">
      <OverviewTab    v-if="activeTab === 'overview'" />
      <MessagesTab    v-else-if="activeTab === 'messages'" />
    </main>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth.js'
import OverviewTab from '../components/OverviewTab.vue'
import MessagesTab from '../components/MessagesTab.vue'

const router = useRouter()
const authStore = useAuthStore()
const activeTab = ref('overview')

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>
