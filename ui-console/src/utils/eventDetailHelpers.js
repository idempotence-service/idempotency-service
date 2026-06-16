export function formatDate(d) {
  if (!d) return '—'
  return new Date(d).toLocaleString('ru-RU', { dateStyle: 'short', timeStyle: 'medium' })
}

export function formatJson(obj) {
  return JSON.stringify(obj, null, 2)
}

export function canRetry(status) {
  return ['ERROR', 'FAILED'].includes(status)
}
