<template>
  <span class="badge" :class="badgeClass">{{ status }}</span>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ status: String })

const badgeClass = computed(() => {
  const s = (props.status || '').toUpperCase()
  if (s.includes('ERROR') || s.includes('FAIL')) return 'badge-error'
  if (s.includes('SUCCESS') || s.includes('COMPLETE') || s.includes('DONE')) return 'badge-success'
  if (s.includes('WAITING') || s.includes('PENDING') || s.includes('PROCESSING')) return 'badge-warning'
  if (s.includes('RETRY') || s.includes('RESEND')) return 'badge-info'
  return 'badge-default'
})
</script>
