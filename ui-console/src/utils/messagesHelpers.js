export function toggleSort(sortBy, sortDir, col) {
  if (sortBy.value === col) {
    sortDir.value = sortDir.value === 'asc' ? 'desc' : 'asc'
  } else {
    sortBy.value = col
    sortDir.value = 'asc'
  }
}

export function typeStyle(type) {
  return {
    sent:      { icon: '↑', label: 'Отправлено', color: '#82b1ff',           bg: 'rgba(130,177,255,0.12)' },
    received:  { icon: '↓', label: 'Получено',   color: 'var(--md-success)', bg: 'rgba(109,213,140,0.12)' },
    error:     { icon: '⚠', label: 'Ошибка',     color: 'var(--md-error)',   bg: 'rgba(242,184,181,0.12)' },
    duplicate: { icon: '♻', label: 'Дубль',      color: '#f6c142',           bg: 'rgba(246,193,66,0.12)'  },
  }[type] ?? { icon: '?', label: type, color: 'var(--md-on-surface-v)', bg: 'var(--md-surface-3)' }
}

export function truncate(s, n) {
  if (!s) return '—'
  return s.length > n ? s.slice(0, 10) + '…' + s.slice(-6) : s
}

export function formatTimestamp(timestamp) {
  if (!timestamp) return '—'
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return '—'
  
  const now = new Date()
  const isToday = date.toDateString() === now.toDateString()
  
  return date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' }) + ' ' + 
         date.toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
}

export function copy(key, toast) {
  navigator.clipboard.writeText(key)
  toast.info('Скопировано')
}

export function filterAndSortMessages(messages, activeFilter, search, sortBy, sortDir, displayLimit) {
  let list = messages
  
  // Filter by type
  if (activeFilter !== 'all') list = list.filter(m => m.type === activeFilter)
  
  // Filter by search
  if (search.trim()) {
    const q = search.trim().toLowerCase()
    list = list.filter(m =>
      m.key?.toLowerCase().includes(q) ||
      m.integration?.toLowerCase().includes(q) ||
      m.status?.toLowerCase().includes(q)
    )
  }
  
  // Sort
  if (sortBy) {
    const col = sortBy
    list = [...list].sort((a, b) => {
      if (col === 'timestamp') {
        const va = a[col] ? new Date(a[col]).getTime() : 0
        const vb = b[col] ? new Date(b[col]).getTime() : 0
        return sortDir === 'asc' ? va - vb : vb - va
      }
      const va = String(a[col] ?? '').toLowerCase()
      const vb = String(b[col] ?? '').toLowerCase()
      return sortDir === 'asc' ? va.localeCompare(vb) : vb.localeCompare(va)
    })
  }
  
  return list.slice(0, displayLimit)
}

export function formatRawFields(selectedMsg) {
  if (!selectedMsg) return []
  const raw = selectedMsg._raw || selectedMsg
  return Object.entries(raw)
    .filter(([k]) => !k.startsWith('_'))
    .map(([k, v]) => [k, v === null || v === undefined ? '—' : (typeof v === 'object' ? JSON.stringify(v, null, 2) : String(v))])
}

export function calculateErrorCountFromAudit(auditActivity) {
  const auditArray = Array.isArray(auditActivity) ? auditActivity : []
  return auditArray.reduce((sum, slot) => {
    return sum +
      (slot['Некорректное входящее событие'] || 0) +
      (slot['Не найден маршрут для входящего события'] || 0) +
      (slot['Некорректный ответ от системы-получателя'] || 0) +
      (slot['Получен ответ без ожидающей операции'] || 0) +
      (slot['Не получен асинхронный ответ от системы-получателя вовремя'] || 0)
  }, 0)
}

export function buildFilters(auditActivity, senderStats, receiverStats, totalDuplicateCount) {
  const errorCountFromAudit = calculateErrorCountFromAudit(auditActivity)

  return [
    { id: 'all',       label: 'Все',         icon: '≡',  count: null,                                                                     activeBg: 'var(--md-surface-3)',    activeColor: 'var(--md-on-surface)' },
    { id: 'sent',      label: 'Отправлены',  icon: '↑',  count: senderStats.totalSent,                                   activeBg: 'rgba(130,177,255,0.2)', activeColor: '#82b1ff' },
    { id: 'received',  label: 'Получены',    icon: '↓',  count: receiverStats.totalReceived,                             activeBg: 'rgba(109,213,140,0.2)', activeColor: 'var(--md-success)' },
    { id: 'error',     label: 'Операции с ошибкой', icon: '⚠',  count: errorCountFromAudit,                                                          activeBg: 'rgba(242,184,181,0.2)', activeColor: 'var(--md-error)' },
    { id: 'duplicate', label: 'Дубли',        icon: '♻',  count: totalDuplicateCount,                                                 activeBg: 'rgba(246,193,66,0.2)',  activeColor: '#f6c142' },
  ]
}

export function isInitialLoading(loading, allMessagesLength) {
  return loading && !allMessagesLength
}

export function isRefreshing(loading, allMessagesLength) {
  return loading && allMessagesLength > 0
}

export function shouldLoadMore(scrollContainer, loadingMore, displayLimit, allMessagesLength, threshold = 100) {
  if (!scrollContainer || loadingMore) return false
  
  const scrollBottom = scrollContainer.scrollHeight - scrollContainer.scrollTop - scrollContainer.clientHeight
  return scrollBottom < threshold && displayLimit < allMessagesLength
}

export function parseApiResponse(data) {
  return Array.isArray(data) ? data : (data ? [data] : [])
}

export function calculateAuditActivitySince() {
  const now = new Date()
  return new Date(now.getTime() - 60 * 60000).toISOString()
}

export function extractErrorMessage(error) {
  if (error?.response?.data?.error?.text) return error.response.data.error.text
  if (error?.message) return error.message
  return 'неизвестно'
}

export function rebuildMessages(sentMessages, receivedEvents, errorEvents, duplicateEvents) {
  const list = []

  // Keys that were sent (to detect duplicates)
  const sentKeySet = new Set(
    sentMessages.map(m => m.uid || m.id || m.globalKey).filter(Boolean)
  )

  sentMessages.forEach(m => {
    const baseKey = m.uid || m.id || JSON.stringify(m)
    list.push({
      _id:         `sent-${baseKey}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:        'sent',
      key:          m.uid || m.id || '',
      integration:  m.integration || '',
      status:       m.status || 'SENT',
      description:  m.description || '',
      timestamp:    m.timestamp || null,
    })
  })

  receivedEvents.forEach(m => {
    const baseKey = m.globalKey || m.uid || JSON.stringify(m)
    list.push({
      _id:         `recv-${baseKey}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:        'received',
      key:          m.globalKey || m.uid || '',
      integration:  m.integration || '',
      status:       m.status || m.result || 'RECEIVED',
      description:  m.resultDescription || '',
      timestamp:    m.timestamp || null,
    })
  })

  errorEvents.forEach(m => {
    const key = m.globalKey || ''
    list.push({
      _id:         `err-${key}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:         'error',
      key,
      integration:  m.integration || '',
      status:       m.status || 'ERROR',
      description:  m.statusDescription || '',
      timestamp:    m.createDate || m.timestamp || null,
    })
  })

  // Add duplicate events from API
  duplicateEvents.forEach(m => {
    const key = m.globalKey || ''
    list.push({
      _id:         `dup-${key}-${Math.random().toString(36).substr(2, 9)}`,
      _raw:         m,
      type:         'duplicate',
      key,
      integration:  m.integration || '',
      status:       'DUPLICATE',
      description:  m.reason || 'Duplicate request blocked by idempotency service',
      timestamp:    m.createDate || m.timestamp || null,
    })
  })

  return list
}
