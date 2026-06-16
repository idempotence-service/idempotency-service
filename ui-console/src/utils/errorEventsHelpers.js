export function truncateKey(key) {
  if (!key) return '—'
  return key.length > 26 ? key.slice(0, 8) + '…' + key.slice(-8) : key
}

export function copyKey(key, toast) {
  navigator.clipboard.writeText(key)
  toast.info('globalKey скопирован')
}

export function toggleSort(sort) {
  return sort === 'desc' ? 'asc' : 'desc'
}

export function calculatePaginationPages(total, cur) {
  if (total <= 7) return Array.from({ length: total }, (_, i) => i)
  const pages = []
  if (cur > 2) pages.push(0, '...')
  for (let i = Math.max(0, cur - 2); i <= Math.min(total - 1, cur + 2); i++) pages.push(i)
  if (cur < total - 3) pages.push('...', total - 1)
  return pages
}

export function filterEvents(events, filterKey, filterIntegration) {
  let list = events
  const qk = filterKey.trim().toLowerCase()
  const qi = filterIntegration.trim().toLowerCase()
  if (qk) list = list.filter(e => e.globalKey?.toLowerCase().includes(qk))
  if (qi) list = list.filter(e => e.integration?.toLowerCase().includes(qi))
  return list
}
